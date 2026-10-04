package com.ecommerce.project.review;

import com.ecommerce.project.review.dto.ReviewDTO;
import com.ecommerce.project.review.dto.ReviewModerationRequest;
import com.ecommerce.project.review.dto.ReviewPageResponse;
import com.ecommerce.project.review.dto.ReviewReplyRequest;
import com.ecommerce.project.review.dto.ReviewRequest;
import com.ecommerce.project.review.dto.ReviewSummaryDTO;

public interface ReviewService {
    ReviewDTO createReview(Long productId, ReviewRequest request);

    ReviewDTO updateReview(Long reviewId, ReviewRequest request);

    void deleteReview(Long reviewId);

    ReviewPageResponse getReviewsForProduct(Long productId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ReviewSummaryDTO getSummaryForProduct(Long productId);

    ReviewPageResponse getMyReviews(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ReviewDTO addSellerReply(Long reviewId, ReviewReplyRequest request);

    ReviewDTO removeSellerReply(Long reviewId);

    ReviewDTO moderateReview(Long reviewId, ReviewModerationRequest request);
}
