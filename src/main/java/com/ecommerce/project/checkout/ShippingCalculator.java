package com.ecommerce.project.checkout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// One flat delivery fee per order, waived from a minimum spend. Applied to the merchandise total
// after any coupon discount. Used by checkout (charged) and the cart (shown in advance).
@Component
public class ShippingCalculator {

    @Value("${app.shipping.fee-minor-units}")
    private long feeMinorUnits;

    @Value("${app.shipping.free-threshold-minor-units}")
    private long freeThresholdMinorUnits;

    public long shippingFor(long merchandiseAfterDiscountMinorUnits, boolean hasItems) {
        if (!hasItems || merchandiseAfterDiscountMinorUnits >= freeThresholdMinorUnits) {
            return 0L;
        }
        return feeMinorUnits;
    }
}
