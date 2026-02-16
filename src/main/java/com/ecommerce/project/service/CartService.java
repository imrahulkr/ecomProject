package com.ecommerce.project.service;

import com.ecommerce.project.payload.CartDTO;

import java.util.List;

public interface CartService {
    public List<CartDTO> getAllCarts();
    public CartDTO addProductToCart(Long productId, Integer quantity);
}
