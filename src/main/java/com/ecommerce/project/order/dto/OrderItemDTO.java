package com.ecommerce.project.order.dto;

import com.ecommerce.project.refund.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecommerce.project.product.dto.ProductDTO;
import com.ecommerce.project.order.FulfillmentStatus;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String orderItemId;
    private ProductDTO product;
    private Integer quantity;
    private Double discount;
    private long priceMinorUnits;
    private long orderedProductPriceMinorUnits;
    private String currency;
    private FulfillmentStatus fulfillmentStatus;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private String returnReason;
    private LocalDateTime returnRequestedAt;
    private RefundStatus refundStatus;
    private long refundedMinorUnits;
    private Instant createdAt;
    private Instant updatedAt;
}
