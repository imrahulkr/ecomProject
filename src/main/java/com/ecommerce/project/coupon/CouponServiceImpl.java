package com.ecommerce.project.coupon;

import com.ecommerce.project.auth.User;
import com.ecommerce.project.cart.Cart;
import com.ecommerce.project.cart.CartRepository;
import com.ecommerce.project.coupon.dto.CouponApplicationResult;
import com.ecommerce.project.coupon.dto.CouponDTO;
import com.ecommerce.project.coupon.dto.CouponPageResponse;
import com.ecommerce.project.coupon.dto.CouponRequest;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.order.Order;
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
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final CartRepository cartRepository;
    private final AuthUtil authUtil;

    @Override
    @Transactional
    public CouponApplicationResult applyCouponToCart(String rawCode) {
        String code = normalizeCode(rawCode);
        User user = authUtil.loggedInUser();
        Cart cart = cartRepository.findCartByEmail(user.getEmail());
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));

        validate(coupon, cart, user);
        long discount = computeDiscount(coupon, cart.getTotalPriceMinorUnits());

        cart.setAppliedCouponCode(coupon.getCode());
        cart.setDiscountMinorUnits(discount);
        cartRepository.save(cart);

        return new CouponApplicationResult(
                coupon.getCode(),
                cart.getTotalPriceMinorUnits(),
                discount,
                cart.getTotalPriceMinorUnits() - discount,
                cart.getCurrency());
    }

    @Override
    @Transactional
    public void removeCouponFromCart() {
        String email = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(email);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", email);
        }
        cart.setAppliedCouponCode(null);
        cart.setDiscountMinorUnits(0L);
        cartRepository.save(cart);
    }

    @Override
    public AppliedCouponDiscount revalidateForCheckout(Cart cart) {
        if (cart.getAppliedCouponCode() == null) {
            return null;
        }
        // Runs inside the checkout transaction - see findByCodeForUpdate.
        Coupon coupon = couponRepository.findByCodeForUpdate(cart.getAppliedCouponCode())
                .orElseThrow(() -> new APIException("Applied coupon is no longer available - please remove it and try again"));
        validate(coupon, cart, cart.getUser());
        long discount = computeDiscount(coupon, cart.getTotalPriceMinorUnits());
        return new AppliedCouponDiscount(coupon, discount);
    }

    @Override
    @Transactional
    public void recordRedemption(Coupon coupon, User user, Order order, long discountMinorUnits) {
        CouponRedemption redemption = new CouponRedemption();
        redemption.setCoupon(coupon);
        redemption.setUser(user);
        redemption.setOrder(order);
        redemption.setDiscountMinorUnits(discountMinorUnits);
        couponRedemptionRepository.save(redemption);
    }

    private void validate(Coupon coupon, Cart cart, User user) {
        if (!coupon.isActive()) {
            throw new APIException("Coupon is not active");
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(Instant.now())) {
            throw new APIException("Coupon has expired");
        }
        if (cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty");
        }
        if (coupon.getMinOrderAmountMinorUnits() != null
                && cart.getTotalPriceMinorUnits() < coupon.getMinOrderAmountMinorUnits()) {
            throw new APIException("Cart does not meet the minimum order amount for this coupon");
        }
        if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT
                && coupon.getCurrency() != null
                && !coupon.getCurrency().equalsIgnoreCase(cart.getCurrency())) {
            throw new APIException("Coupon currency does not match cart currency");
        }
        if (coupon.getMaxRedemptions() != null) {
            long used = couponRedemptionRepository.countActiveRedemptionsForCoupon(coupon.getCouponId());
            if (used >= coupon.getMaxRedemptions()) {
                throw new APIException("Coupon has reached its redemption limit");
            }
        }
        if (coupon.getPerUserLimit() != null) {
            long usedByUser = couponRedemptionRepository
                    .countActiveRedemptionsForCouponAndUser(coupon.getCouponId(), user.getUserId());
            if (usedByUser >= coupon.getPerUserLimit()) {
                throw new APIException("You have already used this coupon the maximum number of times");
            }
        }
    }

    private long computeDiscount(Coupon coupon, long subtotalMinorUnits) {
        long discount = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? Math.round(subtotalMinorUnits * (coupon.getDiscountPercentage() / 100.0))
                : coupon.getDiscountAmountMinorUnits();
        return Math.min(discount, subtotalMinorUnits);
    }

    private String normalizeCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new APIException("Coupon code is required");
        }
        return rawCode.trim().toUpperCase();
    }

    // --- Admin CRUD ---

    @Override
    @Transactional
    public CouponDTO createCoupon(CouponRequest request) {
        validateRequest(request);
        Coupon coupon = new Coupon();
        applyRequest(coupon, request);
        return toDTO(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponDTO updateCoupon(Long couponId, CouponRequest request) {
        validateRequest(request);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "couponId", couponId));
        applyRequest(coupon, request);
        return toDTO(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "couponId", couponId));
        couponRepository.delete(coupon);
    }

    @Override
    public CouponDTO getCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "couponId", couponId));
        return toDTO(coupon);
    }

    @Override
    public CouponPageResponse getAllCoupons(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Coupon> page = couponRepository.findAll(pageable);
        List<CouponDTO> content = page.getContent().isEmpty() ? Collections.emptyList()
                : page.getContent().stream().map(this::toDTO).toList();
        return new CouponPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    private void validateRequest(CouponRequest request) {
        if (request.discountType() == DiscountType.PERCENTAGE && request.discountPercentage() == null) {
            throw new APIException("discountPercentage is required for a PERCENTAGE coupon");
        }
        if (request.discountType() == DiscountType.FIXED_AMOUNT) {
            if (request.discountAmountMinorUnits() == null) {
                throw new APIException("discountAmountMinorUnits is required for a FIXED_AMOUNT coupon");
            }
            if (request.currency() == null || request.currency().isBlank()) {
                throw new APIException("currency is required for a FIXED_AMOUNT coupon");
            }
        }
    }

    private void applyRequest(Coupon coupon, CouponRequest request) {
        coupon.setCode(normalizeCode(request.code()));
        coupon.setDescription(request.description());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountPercentage(request.discountType() == DiscountType.PERCENTAGE ? request.discountPercentage() : null);
        coupon.setDiscountAmountMinorUnits(request.discountType() == DiscountType.FIXED_AMOUNT ? request.discountAmountMinorUnits() : null);
        coupon.setCurrency(request.discountType() == DiscountType.FIXED_AMOUNT ? request.currency() : null);
        coupon.setMinOrderAmountMinorUnits(request.minOrderAmountMinorUnits());
        coupon.setMaxRedemptions(request.maxRedemptions());
        coupon.setPerUserLimit(request.perUserLimit());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setActive(request.active() == null || request.active());
    }

    private CouponDTO toDTO(Coupon coupon) {
        return CouponDTO.builder()
                .couponId(coupon.getCouponId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountPercentage(coupon.getDiscountPercentage())
                .discountAmountMinorUnits(coupon.getDiscountAmountMinorUnits())
                .currency(coupon.getCurrency())
                .minOrderAmountMinorUnits(coupon.getMinOrderAmountMinorUnits())
                .maxRedemptions(coupon.getMaxRedemptions())
                .redemptionCount((int) couponRedemptionRepository.countActiveRedemptionsForCoupon(coupon.getCouponId()))
                .perUserLimit(coupon.getPerUserLimit())
                .expiresAt(coupon.getExpiresAt())
                .active(coupon.isActive())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
