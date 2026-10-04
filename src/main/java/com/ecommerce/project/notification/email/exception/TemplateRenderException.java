package com.ecommerce.project.notification.email.exception;

public class TemplateRenderException extends RuntimeException{
    public TemplateRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
