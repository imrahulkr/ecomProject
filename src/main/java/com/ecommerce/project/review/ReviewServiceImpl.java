package com.ecommerce.project.review;

import com.ecommerce.project.order.OrderItemRepository;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.review.dto.ReviewDTO;
import com.ecommerce.project.review.dto.ReviewModerationRequest;
import com.ecommerce.project.review.dto.ReviewPageResponse;
import com.ecommerce.project.review.dto.ReviewReplyRequest;
import com.ecommerce.project.review.dto.ReviewRequest;
import com.ecommerce.project.review.dto.ReviewSummaryDTO;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public ReviewDTO createReview(Long productId, ReviewRequest request) {
        User user = authUtil.loggedInUser();
        Product product = productRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        if (reviewRepository.existsByProduct_ProductIdAndUser_UserId(productId, user.getUserId())) {
            throw new APIException("You have already reviewed this product");
        }
        // Only buyers may review - otherwise anyone (or a competitor) could flood a product's rating.
        if (!orderItemRepository.hasPurchased(user.getEmail(), productId)) {
            throw new APIException("You can only review products you have bought");
        }
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(Long reviewId, ReviewRequest request) {
        Review review = findOwnedReview(reviewId);
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = findOwnedReview(reviewId);
        reviewRepository.delete(review);
    }

    private Review findOwnedReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));
        User user = authUtil.loggedInUser();
        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new APIException("You can only modify your own review");
        }
        return review;
    }

    @Override
    @Transactional
    public ReviewPageResponse getReviewsForProduct(Long productId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Review> page = reviewRepository.findByProduct_ProductIdAndHiddenFalse(productId, pageable);
        return toPageResponse(page);
    }

    @Override
    public ReviewSummaryDTO getSummaryForProduct(Long productId) {
        ReviewSummary summary = reviewRepository.getSummaryForProduct(productId);
        double average = summary != null && summary.getAverageRating() != null ? summary.getAverageRating() : 0.0;
        long count = summary != null && summary.getReviewCount() != null ? summary.getReviewCount() : 0L;
        return new ReviewSummaryDTO(average, count);
    }

    @Override
    @Transactional
    public ReviewPageResponse getMyReviews(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        User user = authUtil.loggedInUser();
        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Review> page = reviewRepository.findByUser_UserId(user.getUserId(), pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional
    public ReviewDTO addSellerReply(Long reviewId, ReviewReplyRequest request) {
        Long sellerId = authUtil.loggedInUserId();
        Review review = reviewRepository.findByReviewIdAndProduct_User_UserId(reviewId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));
        review.setSellerReply(request.reply());
        review.setSellerRepliedAt(Instant.now());
        return toDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewDTO removeSellerReply(Long reviewId) {
        Long sellerId = authUtil.loggedInUserId();
        Review review = reviewRepository.findByReviewIdAndProduct_User_UserId(reviewId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));
        review.setSellerReply(null);
        review.setSellerRepliedAt(null);
        return toDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewDTO moderateReview(Long reviewId, ReviewModerationRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "reviewId", reviewId));
        review.setHidden(request.hidden());
        review.setHiddenReason(request.hidden() ? request.reason() : null);
        return toDTO(reviewRepository.save(review));
    }

    private ReviewPageResponse toPageResponse(Page<Review> page) {
        List<ReviewDTO> content = page.getContent().isEmpty() ? Collections.emptyList()
                : page.getContent().stream().map(this::toDTO).toList();
        return new ReviewPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    private ReviewDTO toDTO(Review review) {
        return ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .productId(review.getProduct().getProductId())
                .userId(review.getUser().getUserId())
                .userName(review.getUser().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .sellerReply(review.getSellerReply())
                .sellerRepliedAt(review.getSellerRepliedAt())
                .hidden(review.isHidden())
                .hiddenReason(review.getHiddenReason())
                .build();
    }
}
