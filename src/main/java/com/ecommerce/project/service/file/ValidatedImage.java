package com.ecommerce.project.service.file;

public record ValidatedImage(byte[] content, String contentType, String extension) {
}
