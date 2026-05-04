package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.exception.PaymentGatewayValidationException;
import com.neovisionaries.i18n.CurrencyCode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Set;

public class PaymentGatewayEndpointValidation {

  private static final Set<CurrencyCode> ALLOWABLE_COUNTRY_CODES = Set.of(
      CurrencyCode.GBP,
      CurrencyCode.USD,
      CurrencyCode.JPY
  );

  private PaymentGatewayEndpointValidation() {
    throw new IllegalArgumentException("Static validator should not be instantiated");
  }

  public static void validate(PostPaymentRequest paymentRequest) {
    if (!isExpiryDateValid(paymentRequest.expiryMonth(), paymentRequest.expiryYear())) {
      throw new PaymentGatewayValidationException("Card has expired");
    }

    if (!ALLOWABLE_COUNTRY_CODES.contains(paymentRequest.currency())) {
      throw new PaymentGatewayValidationException("Currency code not supported");
    }
  }

  /**
   * Validates whether card expiry date is in the future. Predicate is that a card is valid up until
   * (exclusive) midnight on the day of expiry.
   */
  private static boolean isExpiryDateValid(Integer expiryMonth, Integer expiryYear) {
    YearMonth expiryYearMonth = YearMonth.of(expiryYear, expiryMonth);

    LocalDate dateOfExpiry = expiryYearMonth.atEndOfMonth();
    Instant timeOfExpiry = dateOfExpiry.atTime(LocalTime.MAX)
        .atZone(ZoneId.of("UTC"))
        .toInstant();

    return !Instant.now().isAfter(timeOfExpiry);
  }

}