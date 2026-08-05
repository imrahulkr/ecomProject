package com.ecommerce.project.seller.dto;

import com.ecommerce.project.seller.SellerApplicationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SellerApplicationDTO {
    Long id;
    Long userId;
    String businessName;
    String businessDescription;
    SellerApplicationStatus status;
    String rejectionReason;
    Instant appliedAt;
    Instant decidedAt;
}
