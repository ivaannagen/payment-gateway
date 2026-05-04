package com.checkout.payment.gateway.dto;

import com.neovisionaries.i18n.CurrencyCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PostPaymentRequest(
    @NotNull
    @Pattern(regexp = "^\\d{14,19}$", message = "Invalid card number format")
    String cardNumber,

    @NotNull
    @Min(value = 1, message = "Invalid expiry month")
    @Max(value = 12, message = "Invalid expiry month")
    Integer expiryMonth,

    @NotNull
    Integer expiryYear,

    @NotNull
    @Pattern(regexp = "^\\d{3,4}$", message = "Invalid card number format")
    String cvv,

    @NotNull
    CurrencyCode currency,

    @NotNull
    @Min(value = 1)
    BigDecimal amount) {

}