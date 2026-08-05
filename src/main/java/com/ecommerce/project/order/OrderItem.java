package com.ecommerce.project.order;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecommerce.project.product.Product;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  orderItemId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private Integer quantity;
    private Double price;
    private Double discount;
    private Double orderedProductPrice;

    // Denormalized copy of product.user.userId at order-creation time, matching products.seller_id's
    // naming - kept separate from the product's live owner so ownership-scoped fulfillment queries
    // don't need a join through product, and so a historical order line stays attributable even if
    // the product is later removed.
    @Column(name = "seller_id")
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", nullable = false)
    private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.PENDING;

    private String trackingNumber;
    private String carrier;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

}
