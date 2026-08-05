package com.ecommerce.project.payment;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Spring injects every PaymentProvider bean here (StripePaymentProvider, RazorpayPaymentProvider,
// and any future adapter) automatically - business logic looks providers up by name and never
// references a vendor SDK type directly.
@Component
public class PaymentProviderRegistry {

    private final Map<ProviderName, PaymentProvider> providersByName;

    public PaymentProviderRegistry(List<PaymentProvider> providers) {
        this.providersByName = providers.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentProvider::getProviderName, Function.identity()));
    }

    public PaymentProvider get(ProviderName providerName) {
        PaymentProvider provider = providersByName.get(providerName);
        if (provider == null) {
            throw new IllegalStateException("No PaymentProvider registered for " + providerName);
        }
        return provider;
    }

    public List<ProviderName> allProviderNames() {
        return List.copyOf(providersByName.keySet());
    }
}
