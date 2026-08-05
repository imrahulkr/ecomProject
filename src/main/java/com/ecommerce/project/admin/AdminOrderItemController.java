package com.ecommerce.project.admin;

import com.ecommerce.project.order.OrderService;
import com.ecommerce.project.order.dto.FulfillmentUpdateDTO;
import com.ecommerce.project.order.dto.OrderItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// The "override any seller's line-item fulfillment" admin capability - unlike
// OrderController's seller endpoint, no ownership check, mirroring AdminOrderController's
// whole-order status override.
@RestController
@RequestMapping("/api/admin/order-items")
@RequiredArgsConstructor
public class AdminOrderItemController {

    private final OrderService orderService;

    @PutMapping("/{orderItemId}/fulfillment")
    public ResponseEntity<OrderItemDTO> updateFulfillmentStatus(
            @PathVariable Long orderItemId,
            @RequestBody FulfillmentUpdateDTO update
    ) {
        OrderItemDTO orderItemDTO = orderService.updateFulfillmentStatusAsAdmin(orderItemId, update);
        return new ResponseEntity<>(orderItemDTO, HttpStatus.OK);
    }
}
