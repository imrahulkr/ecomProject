package com.ecommerce.project.wishlist;

import com.ecommerce.project.product.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/wishlist/products/{productId}")
    public ResponseEntity<Void> addToWishlist(@PathVariable Long productId) {
        wishlistService.addToWishlist(productId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/wishlist/products/{productId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(productId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<ProductDTO>> getWishlist() {
        return new ResponseEntity<>(wishlistService.getWishlist(), HttpStatus.OK);
    }
}
