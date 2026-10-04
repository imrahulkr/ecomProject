package com.ecommerce.project.wishlist;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.product.Product;
import com.ecommerce.project.product.ProductRepository;
import com.ecommerce.project.product.ProductService;
import com.ecommerce.project.product.dto.ProductDTO;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public void addToWishlist(Long productId) {
        User user = authUtil.loggedInUser();
        if (wishlistItemRepository.existsByUser_UserIdAndProduct_ProductId(user.getUserId(), productId)) {
            throw new APIException("Product is already in your wishlist");
        }
        Product product = productRepository.findByProductIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        wishlistItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long productId) {
        User user = authUtil.loggedInUser();
        wishlistItemRepository.deleteByUser_UserIdAndProduct_ProductId(user.getUserId(), productId);
    }

    @Override
    public List<ProductDTO> getWishlist() {
        User user = authUtil.loggedInUser();
        List<WishlistItem> items = wishlistItemRepository.findByUser_UserIdAndProduct_ActiveTrueOrderByCreatedAtDesc(user.getUserId());
        return items.stream()
                .map(item -> {
                    try {
                        return productService.getProductById(item.getProduct().getProductId());
                    } catch (ResourceNotFoundException e) {
                        // Product was deleted after being wishlisted - skip rather than fail the whole list.
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
