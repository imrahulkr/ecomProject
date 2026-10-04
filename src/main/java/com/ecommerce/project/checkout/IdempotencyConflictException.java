package com.ecommerce.project.checkout;

import com.ecommerce.project.exceptions.APIException;

public class IdempotencyConflictException extends APIException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
