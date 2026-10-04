package com.ecommerce.project.review;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.order.OrderItemRepository;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.review.dto.ReviewModerationRequest;
import com.ecommerce.project.review.dto.ReviewReplyRequest;
import com.ecommerce.project.review.dto.ReviewRequest;
import com.ecommerce.project.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AuthUtil authUtil;
    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User buyer;
    private User seller;
    private Product product;
    private Review review;

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setUserId(1L);
        buyer.setName("Buyer");

        seller = new User();
        seller.setUserId(2L);

        product = new Product();
        product.setProductId(10L);
        product.setUser(seller);

        review = new Review();
        review.setReviewId(100L);
        review.setProduct(product);
        review.setUser(buyer);
        review.setRating(4);
        review.setComment("Good");
    }

    @Test
    void createReview_rejectsDuplicateReviewForSameProduct() {
        when(authUtil.loggedInUser()).thenReturn(buyer);
        when(productRepository.findByProductIdAndActiveTrue(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProduct_ProductIdAndUser_UserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(10L, new ReviewRequest(5, "Great")))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void createReview_rejectsCustomersWhoHaveNotBoughtTheProduct() {
        when(authUtil.loggedInUser()).thenReturn(buyer);
        when(productRepository.findByProductIdAndActiveTrue(10L)).thenReturn(Optional.of(product));
        when(orderItemRepository.hasPurchased(buyer.getEmail(), 10L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(10L, new ReviewRequest(5, "Great")))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("bought");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_savesForVerifiedBuyers() {
        when(authUtil.loggedInUser()).thenReturn(buyer);
        when(productRepository.findByProductIdAndActiveTrue(10L)).thenReturn(Optional.of(product));
        when(orderItemRepository.hasPurchased(buyer.getEmail(), 10L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.createReview(10L, new ReviewRequest(5, "Great"));

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void updateReview_rejectsWhenCallerDoesNotOwnReview() {
        User otherUser = new User();
        otherUser.setUserId(99L);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(authUtil.loggedInUser()).thenReturn(otherUser);

        assertThatThrownBy(() -> reviewService.updateReview(100L, new ReviewRequest(3, "Meh")))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("your own review");
    }

    @Test
    void addSellerReply_setsReplyWhenCallerOwnsTheProduct() {
        when(authUtil.loggedInUserId()).thenReturn(2L);
        when(reviewRepository.findByReviewIdAndProduct_User_UserId(100L, 2L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = reviewService.addSellerReply(100L, new ReviewReplyRequest("Thanks for the feedback!"));

        assertThat(dto.sellerReply()).isEqualTo("Thanks for the feedback!");
        assertThat(dto.sellerRepliedAt()).isNotNull();
    }

    @Test
    void addSellerReply_throwsNotFoundWhenCallerDoesNotOwnTheProduct() {
        when(authUtil.loggedInUserId()).thenReturn(999L);
        when(reviewRepository.findByReviewIdAndProduct_User_UserId(100L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addSellerReply(100L, new ReviewReplyRequest("Nope")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void moderateReview_hidesReviewAndStoresReason() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = reviewService.moderateReview(100L, new ReviewModerationRequest(true, "Spam"));

        assertThat(dto.hidden()).isTrue();
        assertThat(dto.hiddenReason()).isEqualTo("Spam");
    }

    @Test
    void moderateReview_clearsReasonWhenUnhiding() {
        review.setHidden(true);
        review.setHiddenReason("Spam");
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = reviewService.moderateReview(100L, new ReviewModerationRequest(false, null));

        assertThat(dto.hidden()).isFalse();
        assertThat(dto.hiddenReason()).isNull();
    }
}
