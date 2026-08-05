package com.ecommerce.project.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecommerce.project.product.dto.ProductDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String orderItemId;
    private ProductDTO product;
    private Integer quantity;
    private Double discount;
    private Double price;
    private Double orderedProductPrice;
}
