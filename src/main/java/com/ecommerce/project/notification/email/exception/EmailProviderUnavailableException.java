package com.ecommerce.project.notification.email.exception;

public class EmailProviderUnavailableException extends RuntimeException{
    public EmailProviderUnavailableException(String message) {
        super(message);
    }
}
