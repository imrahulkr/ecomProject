package com.ecommerce.project.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    // Hides soft-deleted products (see Product.active).
    List<WishlistItem> findByUser_UserIdAndProduct_ActiveTrueOrderByCreatedAtDesc(Long userId);

    boolean existsByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.user.userId = ?1 AND w.product.productId = ?2")
    void deleteByUser_UserIdAndProduct_ProductId(Long userId, Long productId);
}
