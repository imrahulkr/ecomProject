package com.ecommerce.project.review;

// JPA projection interface for the aggregate query in ReviewRepository - Spring Data proxies this
// at runtime, no implementation needed.
public interface ReviewSummary {
    Double getAverageRating();
    Long getReviewCount();
}
