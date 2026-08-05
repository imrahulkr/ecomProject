package com.ecommerce.project.checkout.dto;

import com.ecommerce.project.payment.ProviderName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RetryPaymentRequest {

    // Required, not ranked - a retry is an explicit user/frontend choice of provider, never an
    // automatic silent switch (that's how double-charging on an ambiguous timeout gets avoided).
    @NotNull
    private ProviderName provider;
}
