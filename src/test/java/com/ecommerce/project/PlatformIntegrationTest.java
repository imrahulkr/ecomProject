package com.ecommerce.project;

import com.ecommerce.project.auth.AppRole;
import com.ecommerce.project.auth.Role;
import com.ecommerce.project.auth.RoleRepository;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.payment.ProviderName;
import com.ecommerce.project.payment.adapter.stripe.StripePaymentProvider;
import com.ecommerce.project.payment.dto.PaymentIntentResult;
import com.ecommerce.project.payment.dto.PaymentStatusResult;
import com.ecommerce.project.payment.dto.RefundResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

// End-to-end against a real Postgres: the schema is built purely from Flyway migrations
// (V0..latest) and checked by Hibernate's ddl-auto=validate, then the HTTP API is exercised.
// Stripe is a spy with its network calls stubbed. Skipped when Docker isn't available
// (CI provides it). One class on purpose: the container lives as long as the test class, and a
// second class would reuse a cached Spring context pointing at a stopped container.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "it"})
@Testcontainers(disabledWithoutDocker = true)
@SuppressWarnings({"unchecked", "rawtypes"})
class PlatformIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private static final String PASSWORD = "Passw0rd!";

    @LocalServerPort
    private int port;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private StripePaymentProvider stripe;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    // ---- helpers ----

    // In one transaction so the Role entities stay managed (User.roles cascades persist).
    private String createUser(AppRole... roles) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = new User("it" + suffix, "it" + suffix + "@example.com", passwordEncoder.encode(PASSWORD));
            user.setEnabled(true);
            user.setRoles(Arrays.stream(roles)
                    .map(r -> roleRepository.findByRoleName(r).orElseThrow())
                    .collect(Collectors.toSet()));
            userRepository.save(user);
            return user.getEmail();
        });
    }

    private ResponseEntity<Map> login(String email, String password) {
        return http.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .retrieve().toEntity(Map.class);
    }

    private String token(String email) {
        ResponseEntity<Map> res = login(email, PASSWORD);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return (String) res.getBody().get("accessToken");
    }

    private ResponseEntity<Map> call(String method, String uri, String token, Object body) {
        RestClient.RequestBodySpec spec = http.method(org.springframework.http.HttpMethod.valueOf(method)).uri(uri);
        if (token != null) spec = spec.header("Authorization", "Bearer " + token);
        if (body != null) spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
        return spec.retrieve().toEntity(Map.class);
    }

    private int stockOf(Object productId) {
        return ((Number) call("GET", "/api/public/products/" + productId, null, null).getBody().get("quantity")).intValue();
    }

    // ---- tests ----

    @Test
    void schemaFromMigrationsValidates_andHealthIsUp() {
        // Reaching this point means Flyway built the schema and Hibernate validated it.
        assertThat(call("GET", "/actuator/health", null, null).getBody()).containsEntry("status", "UP");
    }

    @Test
    void securityAndErrorResponses() {
        String customer = createUser(AppRole.ROLE_USER);
        String customerToken = token(customer);

        assertThat(call("GET", "/api/orders", null, null).getStatusCode().value()).isEqualTo(401);
        assertThat(call("GET", "/api/admin/app/analytics", customerToken, null).getStatusCode().value()).isEqualTo(403);
        assertThat(call("GET", "/api/auth/user", null, null).getStatusCode().value()).isEqualTo(401);
        assertThat(call("GET", "/api/public/products?sortBy=nope", null, null).getStatusCode().value()).isEqualTo(400);
        assertThat(http.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).body("{bad json")
                .retrieve().toBodilessEntity().getStatusCode().value()).isEqualTo(400);
        // Cookie-authenticated endpoints require the X-Requested-With CSRF guard.
        assertThat(call("POST", "/api/auth/refresh_secure", null, null).getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void repeatedWrongPasswords_lockTheAccount() {
        String email = createUser(AppRole.ROLE_USER);
        for (int i = 0; i < 5; i++) {
            assertThat(login(email, "wrong").getStatusCode().value()).isEqualTo(401);
        }
        ResponseEntity<Map> locked = login(email, PASSWORD);
        assertThat(locked.getStatusCode().value()).isEqualTo(429);
        assertThat(locked.getHeaders().getFirst("Retry-After")).isNotBlank();
    }

    @Test
    void checkout_payWithoutWebhook_thenCancelAndRefund() {
        String admin = createUser(AppRole.ROLE_USER, AppRole.ROLE_SELLER, AppRole.ROLE_ADMIN);
        String customer = createUser(AppRole.ROLE_USER);
        String adminToken = token(admin);
        String customerToken = token(customer);

        // Catalog: one product, 5 in stock, 1,000.00 each (seller = the admin).
        Map category = call("POST", "/api/admin/categories", adminToken,
                Map.of("categoryName", "Category " + UUID.randomUUID().toString().substring(0, 6))).getBody();
        Map product = call("POST", "/api/admin/categories/" + category.get("categoryId") + "/products", adminToken,
                Map.of("productName", "IT product " + UUID.randomUUID().toString().substring(0, 6),
                        "description", "integration test", "priceMinorUnits", 100_000, "discount", 0, "quantity", 5)).getBody();
        Object productId = product.get("productId");

        Map address = call("POST", "/api/addresses", customerToken, Map.of("buildingName", "Building 1",
                "street", "Main Street 12", "city", "Bengaluru", "state", "Karnataka", "country", "India", "pincode", "560001")).getBody();

        Map cart = call("POST", "/api/carts/products/" + productId + "/quantity/2", customerToken, null).getBody();
        assertThat(((Number) cart.get("shippingMinorUnits")).longValue()).isZero(); // 2,000 >= free-delivery threshold
        assertThat(((Number) cart.get("finalPriceMinorUnits")).longValue()).isEqualTo(200_000L);

        // Checkout: Stripe's network calls are stubbed; everything else is real.
        doReturn(PaymentIntentResult.builder().providerName(ProviderName.STRIPE).providerReferenceId("pi_it_1")
                .clientSecret("secret").status("requires_payment_method").clientPayload(Map.of()).build())
                .when(stripe).createPaymentIntent(any());
        Map checkout = http.post().uri("/api/checkout").header("Authorization", "Bearer " + customerToken)
                .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("addressId", address.get("addressId"), "provider", "STRIPE"))
                .retrieve().toEntity(Map.class).getBody();
        Object orderId = checkout.get("orderId");
        assertThat(checkout.get("orderStatus")).isEqualTo("PENDING_PAYMENT");
        assertThat(stockOf(productId)).isEqualTo(3);

        // No webhook: the order page's sync call asks the provider and settles the order.
        doReturn(PaymentStatusResult.builder().state(PaymentStatusResult.State.SUCCEEDED)
                .providerPaymentId("pi_it_1").amountMinorUnits(200_000L).currency("inr").build())
                .when(stripe).fetchStatus("pi_it_1");
        Map order = call("POST", "/api/checkout/" + orderId + "/sync-payment", customerToken, null).getBody();
        assertThat(order.get("orderStatus")).isEqualTo("PAID");

        Map earnings = call("GET", "/api/seller/payouts/summary", adminToken, null).getBody();
        assertThat(((Number) earnings.get("pendingMinorUnits")).longValue()).isEqualTo(180_000L); // minus 10% commission

        // Admin cancels the paid item: customer refunded, stock restored, seller earnings reversed.
        doReturn(RefundResult.builder().providerName(ProviderName.STRIPE).providerRefundId("re_it_1").status("succeeded").build())
                .when(stripe).refund(any());
        Object itemId = ((Map) ((List) order.get("orderItems")).get(0)).get("orderItemId");
        Map item = call("PUT", "/api/admin/order-items/" + itemId + "/fulfillment", adminToken,
                Map.of("status", "CANCELLED")).getBody();
        assertThat(item.get("fulfillmentStatus")).isEqualTo("CANCELLED");
        assertThat(item.get("refundStatus")).isEqualTo("SUCCEEDED");
        assertThat(((Number) item.get("refundedMinorUnits")).longValue()).isEqualTo(200_000L);
        assertThat(stockOf(productId)).isEqualTo(5);
        earnings = call("GET", "/api/seller/payouts/summary", adminToken, null).getBody();
        assertThat(((Number) earnings.get("pendingMinorUnits")).longValue() + ((Number) earnings.get("availableMinorUnits")).longValue()).isZero();

        // A cancelled purchase doesn't count as "bought" for reviews.
        assertThat(call("POST", "/api/products/" + productId + "/reviews", customerToken,
                Map.of("rating", 5, "comment", "great")).getStatusCode().value()).isEqualTo(400);
    }
}
