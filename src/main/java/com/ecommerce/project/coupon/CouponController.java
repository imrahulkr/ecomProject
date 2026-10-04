package com.ecommerce.project.coupon;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.coupon.dto.CouponApplicationResult;
import com.ecommerce.project.coupon.dto.CouponDTO;
import com.ecommerce.project.coupon.dto.CouponPageResponse;
import com.ecommerce.project.coupon.dto.CouponRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // First path is canonical; the second is kept as a deprecated alias for existing clients.
    @PostMapping({"/carts/coupon/{code}", "/cart/coupon/{code}"})
    public ResponseEntity<CouponApplicationResult> applyCoupon(@PathVariable String code) {
        CouponApplicationResult result = couponService.applyCouponToCart(code);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // First path is canonical; the second is kept as a deprecated alias for existing clients.
    @DeleteMapping({"/carts/coupon", "/cart/coupon"})
    public ResponseEntity<Void> removeCoupon() {
        couponService.removeCouponFromCart();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/admin/coupons")
    public ResponseEntity<CouponDTO> createCoupon(@Valid @RequestBody CouponRequest request) {
        return new ResponseEntity<>(couponService.createCoupon(request), HttpStatus.CREATED);
    }

    @PutMapping("/admin/coupons/{couponId}")
    public ResponseEntity<CouponDTO> updateCoupon(@PathVariable Long couponId, @Valid @RequestBody CouponRequest request) {
        return new ResponseEntity<>(couponService.updateCoupon(couponId, request), HttpStatus.OK);
    }

    @DeleteMapping("/admin/coupons/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/admin/coupons/{couponId}")
    public ResponseEntity<CouponDTO> getCoupon(@PathVariable Long couponId) {
        return new ResponseEntity<>(couponService.getCoupon(couponId), HttpStatus.OK);
    }

    @GetMapping("/admin/coupons")
    public ResponseEntity<CouponPageResponse> getAllCoupons(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = "couponId", required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER, required = false) String sortOrder
    ) {
        return new ResponseEntity<>(couponService.getAllCoupons(pageNumber, pageSize, sortBy, sortOrder), HttpStatus.OK);
    }
}
