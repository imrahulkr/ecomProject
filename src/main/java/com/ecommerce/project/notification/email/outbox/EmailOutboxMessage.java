package com.ecommerce.project.notification.email.outbox;

import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "email_outbox")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EmailOutboxMessage {

    private static final Gson GSON = new Gson();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(length = 320)
    private String sender;

    @Column(nullable = false, length = 998)
    private String subject;

    @Column(name = "html_body", columnDefinition = "TEXT")
    private String htmlBody;

    @Column(name = "text_body", columnDefinition = "TEXT")
    private String textBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", length = 64)
    private EmailType emailType;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmailOutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    static EmailOutboxMessage pending(EmailRequest request) {
        EmailOutboxMessage message = new EmailOutboxMessage();
        message.recipient = request.getTo();
        message.sender = request.getFrom();
        message.subject = request.getSubject();
        message.htmlBody = request.getHtmlBody();
        message.textBody = request.getTextBody();
        message.emailType = request.getEmailType();
        message.metadataJson = GSON.toJson(request.getMetadata());
        message.status = EmailOutboxStatus.PENDING;
        message.nextAttemptAt = Instant.now();
        return message;
    }

    EmailRequest toRequest() {
        Map<String, String> metadata = metadataJson == null ? new HashMap<>()
                : GSON.fromJson(metadataJson, new TypeToken<Map<String, String>>() {}.getType());
        return EmailRequest.builder()
                .to(recipient)
                .from(sender)
                .subject(subject)
                .htmlBody(htmlBody)
                .textBody(textBody)
                .emailType(emailType)
                .metadata(metadata)
                .build();
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
