package com.ecommerce.project.wishlist;

import com.ecommerce.project.product.dto.ProductDTO;

import java.util.List;

public interface WishlistService {
    void addToWishlist(Long productId);

    void removeFromWishlist(Long productId);

    List<ProductDTO> getWishlist();
}
