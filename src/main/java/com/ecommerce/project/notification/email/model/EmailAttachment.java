package com.ecommerce.project.notification.email.model;

public class EmailAttachment {
    private String fileName;
    private String contentType;
    private byte[] content;

    public EmailAttachment(String fileName, String contentType, byte[] content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}
