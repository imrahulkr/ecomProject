package com.ecommerce.project.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProduct_ProductId(Long productId, Pageable pageable);

    Page<Review> findByProduct_ProductIdAndHiddenFalse(Long productId, Pageable pageable);

    Page<Review> findByUser_UserId(Long userId, Pageable pageable);

    boolean existsByProduct_ProductIdAndUser_UserId(Long productId, Long userId);

    // Ownership check happens in the WHERE clause, mirroring ProductRepository's
    // findByProductIdAndUser_UserId - a seller can't even see that another seller's product review exists.
    Optional<Review> findByReviewIdAndProduct_User_UserId(Long reviewId, Long sellerId);

    long countByHiddenFalse();

    @Query("SELECT AVG(r.rating) as averageRating, COUNT(r) as reviewCount FROM Review r WHERE r.product.productId = ?1 AND r.hidden = false")
    ReviewSummary getSummaryForProduct(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hidden = false")
    Double getOverallAverageRating();
}
