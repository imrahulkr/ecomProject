package com.ecommerce.project.admin;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.product.ProductService;
import com.ecommerce.project.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// The "view any seller's data" admin capability - unlike GET /api/seller/products, sellerId
// comes from the path, not the caller, since an admin is deliberately looking at someone else's
// catalog rather than their own.
@RestController
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
public class AdminSellerController {

    private final ProductService productService;

    @GetMapping("/{sellerId}/products")
    public ResponseEntity<ProductResponse> getSellerProducts(
            @PathVariable Long sellerId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ) {
        ProductResponse productResponse = productService.getAllProductsBySellerId(sellerId, pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.status(HttpStatus.OK).body(productResponse);
    }
}
