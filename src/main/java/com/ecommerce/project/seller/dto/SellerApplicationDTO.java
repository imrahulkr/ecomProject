package com.ecommerce.project.seller.dto;

import com.ecommerce.project.seller.SellerApplicationStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SellerApplicationDTO(
        Long id,
        Long userId,
        String businessName,
        String businessDescription,
        SellerApplicationStatus status,
        String rejectionReason,
        Instant appliedAt,
        Instant decidedAt
) {}
