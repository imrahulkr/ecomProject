package com.ecommerce.project.review;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.review.dto.ReviewDTO;
import com.ecommerce.project.review.dto.ReviewModerationRequest;
import com.ecommerce.project.review.dto.ReviewPageResponse;
import com.ecommerce.project.review.dto.ReviewReplyRequest;
import com.ecommerce.project.review.dto.ReviewRequest;
import com.ecommerce.project.review.dto.ReviewSummaryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ReviewDTO> createReview(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return new ResponseEntity<>(reviewService.createReview(productId, request), HttpStatus.CREATED);
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewRequest request) {
        return new ResponseEntity<>(reviewService.updateReview(reviewId, request), HttpStatus.OK);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/users/reviews")
    public ResponseEntity<ReviewPageResponse> getMyReviews(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = "reviewId", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ) {
        return new ResponseEntity<>(reviewService.getMyReviews(pageNumber, pageSize, sortBy, sortOrder), HttpStatus.OK);
    }

    @GetMapping("/public/products/{productId}/reviews")
    public ResponseEntity<ReviewPageResponse> getReviewsForProduct(
            @PathVariable Long productId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = "reviewId", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ) {
        return new ResponseEntity<>(reviewService.getReviewsForProduct(productId, pageNumber, pageSize, sortBy, sortOrder), HttpStatus.OK);
    }

    @GetMapping("/public/products/{productId}/reviews/summary")
    public ResponseEntity<ReviewSummaryDTO> getReviewSummary(@PathVariable Long productId) {
        return new ResponseEntity<>(reviewService.getSummaryForProduct(productId), HttpStatus.OK);
    }

    @PutMapping("/seller/reviews/{reviewId}/reply")
    public ResponseEntity<ReviewDTO> replyToReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewReplyRequest request) {
        return new ResponseEntity<>(reviewService.addSellerReply(reviewId, request), HttpStatus.OK);
    }

    @DeleteMapping("/seller/reviews/{reviewId}/reply")
    public ResponseEntity<ReviewDTO> deleteReplyToReview(@PathVariable Long reviewId) {
        return new ResponseEntity<>(reviewService.removeSellerReply(reviewId), HttpStatus.OK);
    }

    @PutMapping("/admin/reviews/{reviewId}/moderate")
    public ResponseEntity<ReviewDTO> moderateReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewModerationRequest request) {
        return new ResponseEntity<>(reviewService.moderateReview(reviewId, request), HttpStatus.OK);
    }
}
