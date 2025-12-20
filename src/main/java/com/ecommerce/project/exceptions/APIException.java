package com.ecommerce.project.exceptions;

public class APIException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public APIException(String message, Throwable cause) {
        super(message, cause);
    }
    public APIException(String message) {
        super(message);
    }
    public APIException(){}
}
