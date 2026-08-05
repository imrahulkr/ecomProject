package com.ecommerce.project.checkout.dto;

import com.ecommerce.project.payment.ProviderName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotNull
    private Long addressId;

    // Optional - when omitted, the provider is picked automatically from health ranking.
    private ProviderName provider;
}
