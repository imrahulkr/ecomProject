package com.ecommerce.project.review.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ReviewDTO(
        Long reviewId,
        Long productId,
        Long userId,
        String userName,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        String sellerReply,
        Instant sellerRepliedAt,
        boolean hidden,
        String hiddenReason
) {}
