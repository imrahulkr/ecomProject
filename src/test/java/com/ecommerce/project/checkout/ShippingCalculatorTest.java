package com.ecommerce.project.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingCalculatorTest {

    private final ShippingCalculator calculator = new ShippingCalculator();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(calculator, "feeMinorUnits", 4_900L);
        ReflectionTestUtils.setField(calculator, "freeThresholdMinorUnits", 49_900L);
    }

    @Test
    void chargesTheFeeBelowTheThreshold() {
        assertThat(calculator.shippingFor(49_899L, true)).isEqualTo(4_900L);
    }

    @Test
    void isFreeFromTheThreshold() {
        assertThat(calculator.shippingFor(49_900L, true)).isZero();
    }

    @Test
    void emptyCartHasNoShipping() {
        assertThat(calculator.shippingFor(0L, false)).isZero();
    }
}
