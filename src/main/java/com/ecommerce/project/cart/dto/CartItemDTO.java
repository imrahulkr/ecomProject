package com.ecommerce.project.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecommerce.project.product.dto.ProductDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO {
//    private Long cartItemId;
//    private CartDTO cart;
//    private ProductDTO product;
//    private Integer quantity;
//    private Double discount;
//    private Double productPrice;

    private Long productId;
    private Integer quantity;
}
