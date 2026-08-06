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
    private List<ProductDTO> products = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
