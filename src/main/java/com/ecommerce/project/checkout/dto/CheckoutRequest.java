package com.ecommerce.project.checkout.dto;

import com.ecommerce.project.payment.ProviderName;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long addressId,

        // Optional - when omitted, the provider is picked automatically from health ranking.
        ProviderName provider
) {}
