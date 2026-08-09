package com.ecommerce.project.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long productId;

    @NotBlank
    @Size(min = 3, message = "Product name must contain at least 3 characters")
    private String productName;

    private String description;
    private String image;

    @NotNull
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull
    @Positive(message = "Price must be greater than 0")
    private Long priceMinorUnits;

    @NotNull
    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    @DecimalMax(value = "100.0", message = "Discount cannot exceed 100")
    private Double discount;

    // Always server-computed from priceMinorUnits/discount (see ProductServiceImpl) -- boxed so a
    // client omitting it on create/update doesn't fail JSON deserialization outright.
    private Long specialPriceMinorUnits;

    private String currency;
    private Long categoryId;
    private Long sellerId;
    private String sellerName;
    private Instant createdAt;
    private Instant updatedAt;
}
