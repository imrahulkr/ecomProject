package com.ecommerce.project.notification.email.service;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.order.Order;
import com.ecommerce.project.order.OrderItem;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.notification.email.model.EmailPriority;
import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailType;
import com.ecommerce.project.notification.email.provider.EmailProvider;
import com.ecommerce.project.notification.email.template.EmailTemplateEngine;
import com.ecommerce.project.payment.PaymentAttempt;
import com.ecommerce.project.payment.PaymentAttemptRepository;
import com.ecommerce.project.payment.PaymentAttemptStatus;
import com.ecommerce.project.seller.SellerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final EmailTemplateEngine templateEngine;
    private final EmailRetryService emailRetryService;
    private final PaymentAttemptRepository paymentAttemptRepository;

    @Value("${app.email.default-from}")
    private String defaultFrom;
    @Value("${app.base.url}")
    private String baseUrl;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailServiceImpl(EmailTemplateEngine templateEngine, EmailRetryService emailRetryService,
                             PaymentAttemptRepository paymentAttemptRepository) {
        this.templateEngine = templateEngine;
        this.emailRetryService = emailRetryService;
        this.paymentAttemptRepository = paymentAttemptRepository;
    }

    @Override
    public void sendVerificationEmail(User user, String token) {
        Map<String, Object> vars = Map.of(
                "name", user.getUsername(),
                "verificationUrl",  baseUrl +"/api/auth/verify?token=" + token
        );
        logger.debug("Sending verification email to name={}, url={}", vars.get("name"), vars.get("verificationUrl"));
        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Verify your account")
                .htmlBody(templateEngine.render("verification-email", vars))
                .emailType(EmailType.VERIFICATION)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("userId", String.valueOf(user.getUserId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendPasswordResetEmail(User user, String token) {
        Map<String, Object> vars = Map.of(
                "name", user.getUsername(),
                "verificationUrl",  baseUrl +"/reset-password?token=" + token
        );

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Reset your Password")
                .htmlBody(templateEngine.render("password-reset", vars))
                .emailType(EmailType.PASSWORD_RESET)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("userId", String.valueOf(user.getUserId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendPasswordChangedEmail(User user) {
        Map<String, Object> vars = Map.of(
                "name", user.getUsername()
        );

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Reset your Password")
                .htmlBody(templateEngine.render("password-changed", vars))
                .emailType(EmailType.PASSWORD_RESET)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("userId", String.valueOf(user.getUserId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendOrderConfirmation(Order order) {
        String name = customerName(order);
        String paymentMethod = latestSucceededProviderName(order.getOrderId());

        Map<String, Object> vars = Map.of(
                "name", name,
                "orderNumber", String.valueOf(order.getOrderId()),
                "orderDate", order.getOrderDate() != null
                        ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) : "",
                "paymentMethod", paymentMethod,
                "totalAmount", String.format("%.2f", order.getTotalAmount()),
                "trackingUrl", frontendUrl + "/orders/" + order.getOrderId()
        );

        EmailRequest request = EmailRequest.builder()
                .to(order.getEmail())
                .from(defaultFrom)
                .subject("Order Confirmed")
                .htmlBody(templateEngine.render("order-confirmation", vars))
                .emailType(EmailType.ORDER_CONFIRMATION)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("orderId", String.valueOf(order.getOrderId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendPaymentFailedEmail(Order order, String reason) {
        Map<String, Object> vars = Map.of(
                "name", customerName(order),
                "orderNumber", String.valueOf(order.getOrderId()),
                "reason", reason != null ? reason : "Payment was declined by the payment provider",
                "retryUrl", frontendUrl + "/orders/" + order.getOrderId()
        );

        EmailRequest request = EmailRequest.builder()
                .to(order.getEmail())
                .from(defaultFrom)
                .subject("Payment Unsuccessful for your order")
                .htmlBody(templateEngine.render("payment-failed", vars))
                .emailType(EmailType.PAYMENT_FAILED)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("orderId", String.valueOf(order.getOrderId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendShippingUpdate(OrderItem orderItem) {
        Order order = orderItem.getOrder();
        Map<String, Object> vars = Map.of(
                "name", customerName(order),
                "orderNumber", String.valueOf(order.getOrderId()),
                "productName", orderItem.getProduct().getProductName(),
                "trackingNumber", orderItem.getTrackingNumber() != null ? orderItem.getTrackingNumber() : "N/A",
                "carrier", orderItem.getCarrier() != null ? orderItem.getCarrier() : "N/A",
                "trackingUrl", frontendUrl + "/orders/" + order.getOrderId()
        );

        EmailRequest request = EmailRequest.builder()
                .to(order.getEmail())
                .from(defaultFrom)
                .subject("Your item has shipped")
                .htmlBody(templateEngine.render("shipping-update", vars))
                .emailType(EmailType.SHIPPING_UPDATE)
                .priority(EmailPriority.NORMAL)
                .metadata(Map.of("orderId", String.valueOf(order.getOrderId()),
                        "orderItemId", String.valueOf(orderItem.getOrderItemId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendDeliveryConfirmation(OrderItem orderItem) {
        Order order = orderItem.getOrder();
        Map<String, Object> vars = Map.of(
                "name", customerName(order),
                "orderNumber", String.valueOf(order.getOrderId()),
                "productName", orderItem.getProduct().getProductName(),
                "trackingUrl", frontendUrl + "/orders/" + order.getOrderId()
        );

        EmailRequest request = EmailRequest.builder()
                .to(order.getEmail())
                .from(defaultFrom)
                .subject("Your item has been delivered")
                .htmlBody(templateEngine.render("delivery-confirmation", vars))
                .emailType(EmailType.DELIVERY_CONFIRMATION)
                .priority(EmailPriority.NORMAL)
                .metadata(Map.of("orderId", String.valueOf(order.getOrderId()),
                        "orderItemId", String.valueOf(orderItem.getOrderItemId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    private String customerName(Order order) {
        if (order.getAddress() != null && order.getAddress().getUser() != null
                && order.getAddress().getUser().getUsername() != null) {
            return order.getAddress().getUser().getUsername();
        }
        return order.getEmail();
    }

    private String latestSucceededProviderName(Long orderId) {
        return paymentAttemptRepository.findByOrder_OrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(attempt -> attempt.getStatus() == PaymentAttemptStatus.SUCCEEDED)
                .findFirst()
                .map(PaymentAttempt::getProviderName)
                .map(Enum::name)
                .orElse("N/A");
    }

    @Override
    public void sendSellerApplicationApprovedEmail(SellerApplication application) {
        User user = application.getUser();
        Map<String, Object> vars = Map.of(
                "name", user.getUsername(),
                "businessName", application.getBusinessName(),
                "sellerDashboardUrl", frontendUrl + "/seller/dashboard"
        );

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Your seller application has been approved")
                .htmlBody(templateEngine.render("seller-application-approved", vars))
                .emailType(EmailType.SELLER_APPLICATION_APPROVED)
                .priority(EmailPriority.HIGH)
                .metadata(Map.of("userId", String.valueOf(user.getUserId()),
                        "applicationId", String.valueOf(application.getId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendSellerApplicationRejectedEmail(SellerApplication application) {
        User user = application.getUser();
        Map<String, Object> vars = Map.of(
                "name", user.getUsername(),
                "businessName", application.getBusinessName(),
                "reason", application.getRejectionReason() != null
                        ? application.getRejectionReason() : "Not specified"
        );

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("Update on your seller application")
                .htmlBody(templateEngine.render("seller-application-rejected", vars))
                .emailType(EmailType.SELLER_APPLICATION_REJECTED)
                .priority(EmailPriority.NORMAL)
                .metadata(Map.of("userId", String.valueOf(user.getUserId()),
                        "applicationId", String.valueOf(application.getId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }

    @Override
    public void sendAbandonedCartReminder(User user, Cart cart) {
        List<Map<String, Object>> items = cart.getCartItems().stream()
                .map(item -> Map.<String, Object>of(
                        "productName", item.getProduct().getProductName(),
                        "quantity", item.getQuantity()
                ))
                .toList();

        Map<String, Object> vars = Map.of(
                "name", user.getUsername(),
                "items", items,
                "cartUrl", frontendUrl + "/cart"
        );

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .from(defaultFrom)
                .subject("You left something in your cart")
                .htmlBody(templateEngine.render("abandoned-cart", vars))
                .emailType(EmailType.ABANDONED_CART)
                .priority(EmailPriority.NORMAL)
                .metadata(Map.of("userId", String.valueOf(user.getUserId()), "cartId", String.valueOf(cart.getCartId())))
                .build();
        emailRetryService.sendEmailWithRetry(request);
    }
}
