package com.ecommerce.project.checkout;

import com.ecommerce.project.checkout.dto.CheckoutRequest;
import com.ecommerce.project.checkout.dto.CheckoutResponse;
import com.ecommerce.project.checkout.dto.RetryPaymentRequest;
import com.ecommerce.project.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final AuthUtil authUtil;

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request
    ) {
        CheckoutResponse response = checkoutService.checkout(
                authUtil.loggedInUserId(), authUtil.loggedInEmail(), idempotencyKey, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{orderId}/retry-payment")
    public ResponseEntity<CheckoutResponse> retryPayment(
            @PathVariable Long orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RetryPaymentRequest request
    ) {
        CheckoutResponse response = checkoutService.retryPayment(
                authUtil.loggedInUserId(), authUtil.loggedInEmail(), orderId, idempotencyKey, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
