package com.ecommerce.project.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long productId;
    private String productName;
    private String description;
    private String image;
    private Integer quantity;
    private long priceMinorUnits;
    private Double discount;
    private long specialPriceMinorUnits;
    private String currency;
    private Long categoryId;
    private Long sellerId;
    private String sellerName;
    private Instant createdAt;
    private Instant updatedAt;
}
