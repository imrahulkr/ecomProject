package com.ecommerce.project.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {
}
