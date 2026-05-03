package com.yas.product.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

class PriceValidatorTest {

    private final PriceValidator validator = new PriceValidator();

    @Test
    void isValid_whenPositive_thenTrue() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertThat(validator.isValid(10.0, context)).isTrue();
    }

    @Test
    void isValid_whenZero_thenTrue() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertThat(validator.isValid(0.0, context)).isTrue();
    }

    @Test
    void isValid_whenNegative_thenFalse() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        assertThat(validator.isValid(-1.0, context)).isFalse();
    }
}
