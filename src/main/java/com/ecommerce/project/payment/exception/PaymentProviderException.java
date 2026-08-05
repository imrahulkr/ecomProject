package com.ecommerce.project.payment.exception;

import com.ecommerce.project.payment.ProviderName;

public class PaymentProviderException extends RuntimeException {
    private final ProviderName providerName;
    private final boolean retryable;

    public PaymentProviderException(ProviderName providerName, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.retryable = retryable;
    }

    public ProviderName getProviderName() {
        return providerName;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
