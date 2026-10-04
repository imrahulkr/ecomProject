package com.ecommerce.project.cart.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.ecommerce.project.product.dto.ProductDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private Long cartId;
    private long totalPriceMinorUnits = 0L;
    private String currency;
    private String appliedCouponCode;
    private long discountMinorUnits = 0L;
    // Delivery fee checkout will add for the current cart (checkout.ShippingCalculator).
    private long shippingMinorUnits = 0L;
    // What checkout will charge: items - coupon discount + shipping.
    private long finalPriceMinorUnits = 0L;
    private List<ProductDTO> products = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    // Derived, not persisted separately - keeps totalPriceMinorUnits as the pure line-item subtotal
    // (unchanged by coupon logic) while still exposing what the customer will actually pay.
    public long getFinalPriceMinorUnits() {
        return Math.max(0L, totalPriceMinorUnits - discountMinorUnits);
    }
}
