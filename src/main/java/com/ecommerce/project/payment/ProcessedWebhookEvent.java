package com.ecommerce.project.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_webhook_event")
@Getter
@NoArgsConstructor
public class ProcessedWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_name", nullable = false, length = 32)
    private ProviderName providerName;

    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedWebhookEvent(ProviderName providerName, String providerEventId) {
        this.providerName = providerName;
        this.providerEventId = providerEventId;
        this.processedAt = Instant.now();
    }
}
