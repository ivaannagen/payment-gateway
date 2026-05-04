package com.checkout.payment.gateway.dto;

import com.neovisionaries.i18n.CurrencyCode;
import java.math.BigDecimal;
import java.util.UUID;

public record PostPaymentResponse(
    UUID id,
    String status,
    String cardNumberLastFour,
    Integer expiryMonth,
    Integer expiryYear,
    CurrencyCode currency,
    BigDecimal amount
) {
}