package com.ecommerce.project.payment.exception;

import com.ecommerce.project.payment.ProviderName;

public class ProviderUnavailableException extends PaymentProviderException {
    public ProviderUnavailableException(ProviderName providerName) {
        super(providerName, providerName + " is temporarily unavailable (in cooldown after repeated failures)", true, null);
    }
}
