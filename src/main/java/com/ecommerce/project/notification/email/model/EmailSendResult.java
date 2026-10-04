package com.ecommerce.project.notification.email.model;

public class EmailSendResult {
    private boolean success;
    private String providerMessageId;
    private String errorMessage;
    private String providerUsed;

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final  EmailSendResult emailSendResult = new EmailSendResult();
        public Builder success(boolean success) {emailSendResult.success = success; return this;}
        public Builder providerMessageId(String providerMessageId) {emailSendResult.providerMessageId = providerMessageId; return this;}
        public Builder errorMessage(String errorMessage) {emailSendResult.errorMessage = errorMessage; return this;}
        public Builder  providerUsed(String providerUsed) {emailSendResult.providerUsed = providerUsed; return this;}
        public  EmailSendResult build() {return emailSendResult;}
    }
    public boolean isSuccess() {return success;}
    public String getProviderMessageId() {return providerMessageId;}
    public String getErrorMessage() {return errorMessage;}
    public String getProviderUsed() {return providerUsed;}
}
