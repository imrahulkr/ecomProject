package com.ecommerce.project.cart;

import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.dto.CartDTO;
import com.ecommerce.project.cart.dto.CartItemDTO;
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
