package com.ecommerce.project.order;

import jakarta.validation.Valid;
import com.ecommerce.project.order.dto.ReturnRequestDTO;
import com.ecommerce.project.refund.RefundService;

import com.ecommerce.project.checkout.PaymentReconciliationService;
import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.project.order.dto.FulfillmentUpdateDTO;
import com.ecommerce.project.order.dto.OrderDTO;
import com.ecommerce.project.order.dto.OrderItemDTO;
import com.ecommerce.project.order.dto.OrderResponse;

// Admin order listing/override moved to admin.AdminOrderController - see its comment for why.
// The seller order-status endpoint that used to live here was removed outright: it let any
// seller change the status of any order by id with no ownership check, and it overwrote the
// order's customer email as a side effect. Real seller-scoped fulfillment control lands in
// Phase 7 once fulfillment status is tracked per line item instead of per whole order.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final AuthUtil authUtil;
    private final OrderService orderService;
    private final PaymentReconciliationService paymentReconciliationService;
    private final RefundService refundService;

    @GetMapping("/orders")
    public ResponseEntity<OrderResponse> getMyOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDER_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ) {
        String emailId = authUtil.loggedInEmail();
        OrderResponse orderResponse = orderService.getOrdersForUser(emailId, pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long orderId) {
        String emailId = authUtil.loggedInEmail();
        OrderDTO orderDTO = orderService.getOrderByIdForUser(emailId, orderId);
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long orderId) {
        String emailId = authUtil.loggedInEmail();
        // Cancelling also has to stop the provider-side payment, which is a network call that
        // can't run inside OrderService's transactions - see PaymentReconciliationService.
        paymentReconciliationService.cancelOrderForUser(emailId, orderId);
        return new ResponseEntity<>(orderService.getOrderByIdForUser(emailId, orderId), HttpStatus.OK);
    }

    // Customer asks to return a delivered item (seller/admin then approves -> refund, or rejects).
    @PostMapping("/orders/{orderId}/items/{orderItemId}/return")
    public ResponseEntity<OrderItemDTO> requestReturn(@PathVariable Long orderId, @PathVariable Long orderItemId,
                                                      @Valid @RequestBody ReturnRequestDTO request) {
        return ResponseEntity.ok(orderService.requestReturn(authUtil.loggedInEmail(), orderId, orderItemId, request.reason()));
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

    @PutMapping("/seller/order-items/{orderItemId}/fulfillment")
    public ResponseEntity<OrderItemDTO> updateFulfillmentStatus(
            @PathVariable Long orderItemId,
            @RequestBody FulfillmentUpdateDTO update
    ) {
        Long sellerId = authUtil.loggedInUser().getUserId();
        if (RefundService.isRefunding(update.status())) {
            return ResponseEntity.ok(refundService.refundItem(orderItemId, sellerId, update.status(),
                    RefundService.reasonFor(update.status(), "seller")));
        }
        OrderItemDTO orderItemDTO = orderService.updateFulfillmentStatusAsSeller(sellerId, orderItemId, update);
        return new ResponseEntity<>(orderItemDTO, HttpStatus.OK);
    }
}
