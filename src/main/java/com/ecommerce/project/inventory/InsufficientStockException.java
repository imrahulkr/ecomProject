package com.ecommerce.project.inventory;

import com.ecommerce.project.exceptions.APIException;

public class InsufficientStockException extends APIException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
