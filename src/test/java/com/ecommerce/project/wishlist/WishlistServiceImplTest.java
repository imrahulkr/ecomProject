package com.ecommerce.project.wishlist;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.product.ProductService;
import com.ecommerce.project.product.dto.ProductDTO;
import com.ecommerce.project.util.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductService productService;
    @Mock
    private AuthUtil authUtil;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        product = new Product();
        product.setProductId(10L);
    }

    @Test
    void addToWishlist_rejectsDuplicateEntry() {
        when(authUtil.loggedInUser()).thenReturn(user);
        when(wishlistItemRepository.existsByUser_UserIdAndProduct_ProductId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.addToWishlist(10L))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("already in your wishlist");
    }

    @Test
    void addToWishlist_savesItemWhenProductExists() {
        when(authUtil.loggedInUser()).thenReturn(user);
        when(wishlistItemRepository.existsByUser_UserIdAndProduct_ProductId(1L, 10L)).thenReturn(false);
        when(productRepository.findByProductIdAndActiveTrue(10L)).thenReturn(Optional.of(product));

        wishlistService.addToWishlist(10L);

        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void addToWishlist_throwsWhenProductDoesNotExist() {
        when(authUtil.loggedInUser()).thenReturn(user);
        when(wishlistItemRepository.existsByUser_UserIdAndProduct_ProductId(1L, 10L)).thenReturn(false);
        when(productRepository.findByProductIdAndActiveTrue(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getWishlist_skipsItemsWhoseProductWasSinceDeleted() {
        WishlistItem live = new WishlistItem();
        live.setProduct(product);
        Product deletedProduct = new Product();
        deletedProduct.setProductId(20L);
        WishlistItem stale = new WishlistItem();
        stale.setProduct(deletedProduct);

        when(authUtil.loggedInUser()).thenReturn(user);
        when(wishlistItemRepository.findByUser_UserIdAndProduct_ActiveTrueOrderByCreatedAtDesc(1L)).thenReturn(List.of(live, stale));

        ProductDTO liveDto = new ProductDTO();
        liveDto.setProductId(10L);
        when(productService.getProductById(10L)).thenReturn(liveDto);
        when(productService.getProductById(20L)).thenThrow(new ResourceNotFoundException("Product", "productId", 20L));

        List<ProductDTO> wishlist = wishlistService.getWishlist();

        assertThat(wishlist).containsExactly(liveDto);
    }
}
