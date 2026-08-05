package com.ecommerce.project.order;


import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.security.dto.JwtPrincipal;
import com.ecommerce.project.payment.StripeService;
import com.ecommerce.project.util.AuthUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.project.order.dto.OrderDTO;
import com.ecommerce.project.order.dto.OrderRequestDTO;
import com.ecommerce.project.order.dto.OrderResponse;
import com.ecommerce.project.order.dto.OrderStatusUpdateDTO;
import com.ecommerce.project.payment.dto.StripePaymentDTO;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final AuthUtil authUtil;
    private final StripeService stripeService;
    private final OrderService orderService;

    @PostMapping("/order/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod,
                                                  @RequestBody OrderRequestDTO orderRequestDTO) {
        String emailId = authUtil.loggedInEmail();
        OrderDTO orderDTO =  orderService.placeOrder(emailId, orderRequestDTO.getAddressId(), paymentMethod,
                orderRequestDTO.getPgName(), orderRequestDTO.getPgPaymentId(),
                orderRequestDTO.getPgStatus(), orderRequestDTO.getPgResponseMessage());
        return new ResponseEntity<>(orderDTO, HttpStatus.CREATED);
    }

    @PostMapping("/order/stripe-client-secret")
    public ResponseEntity<String> createStripeClientSecret(@RequestBody StripePaymentDTO stripePaymentDTO) throws StripeException {
        PaymentIntent paymentIntent = stripeService.paymentIntent(stripePaymentDTO);
        return new ResponseEntity<>(paymentIntent.getClientSecret(), HttpStatus.CREATED);
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<OrderResponse> getAllOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDER_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ){
        OrderResponse orderResponse = orderService.getAllOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @GetMapping("/seller/orders")
    public ResponseEntity<OrderResponse> getAllSellerOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDER_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ){
        OrderResponse orderResponse = orderService.getAllSellerOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @PutMapping("/seller/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatusSeller(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO,
            Authentication authentication
    ){
        JwtPrincipal userDetails= (JwtPrincipal) authentication.getPrincipal();
        String emailId = userDetails.email();

        OrderDTO orderDTO = orderService.updateOrder(emailId, orderId, orderStatusUpdateDTO.getStatus());
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }

    @PutMapping("/admin/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO,
            Authentication authentication
    ){
        JwtPrincipal userDetails= (JwtPrincipal) authentication.getPrincipal();
        String emailId = userDetails.email();

        OrderDTO orderDTO = orderService.updateOrder(emailId, orderId, orderStatusUpdateDTO.getStatus());
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }
}
