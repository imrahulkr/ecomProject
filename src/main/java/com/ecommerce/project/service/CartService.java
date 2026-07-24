package com.ecommerce.project.service;

import com.ecommerce.project.model.Cart;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface CartService {
    public List<CartDTO> getAllCarts();
    public CartDTO addProductToCart(Long productId, Integer quantity);

    CartDTO getCart(String emailId, Long cartId);
    @Transactional
    CartDTO updateCartProduct(Long productId, Integer quantity);

    String deleteProductFromCart(Long cartId, Long productId);

    String createOrUpdateCartWithItems(List<CartItemDTO> cartItems);
}
