package com.ecommerce.project.notification.email.repository;

import com.ecommerce.project.notification.email.model.EmailRequest;
import com.ecommerce.project.notification.email.model.EmailSendResult;
import com.ecommerce.project.notification.email.model.EmailStatus;
import com.ecommerce.project.notification.email.model.EmailType;
import com.google.gson.Gson;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String recipient;

    @Enumerated(EnumType.STRING)
    private EmailType emailType;
    private String providerUsed;
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    private EmailStatus status;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private int retrycount;
    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime sentAt;

    public static EmailLog from(EmailRequest request, EmailSendResult result){
        EmailLog emailLog = new EmailLog();
        emailLog.recipient = request.getTo();
        emailLog.emailType = request.getEmailType();
        emailLog.providerUsed = result.getProviderUsed();
        emailLog.providerMessageId = result.getProviderMessageId();
        emailLog.status = result.isSuccess() ? EmailStatus.SENT : EmailStatus.FAILED;
        emailLog.errorMessage = result.getErrorMessage();
        emailLog.sentAt = result.isSuccess() ? LocalDateTime.now() : null;
        emailLog.metadataJson = new Gson().toJson(request.getMetadata());
        return emailLog;
    }

    public String getProviderUsed() {
        return providerUsed;
    }

    public void setProviderUsed(String providerUsed) {
        this.providerUsed = providerUsed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public EmailType getEmailType() {
        return emailType;
    }

    public void setEmailType(EmailType emailType) {
        this.emailType = emailType;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetrycount() {
        return retrycount;
    }

    public void setRetrycount(int retrycount) {
        this.retrycount = retrycount;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
