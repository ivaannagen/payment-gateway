package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.exception.PaymentGatewayValidationException;
import com.neovisionaries.i18n.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.checkout.payment.gateway.TestUtils.VALID_EXPIRY_YEAR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayEndpointValidationTest {

  @Test
  void shouldThrowExceptionWhenCurrencyCodeNotSupported() {
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248877",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.EUR,
        new BigDecimal("15.80")
    );

    assertThatThrownBy(() -> PaymentGatewayEndpointValidation.validate(paymentRequest))
        .isInstanceOf(PaymentGatewayValidationException.class)
        .hasMessageContaining("Currency code not supported");
  }

  @Test
  void shouldThrowExceptionWhenCardExpiryDateIsInNotInFuture() {
    LocalDate currentDate = Instant.now().atZone(ZoneId.of("UTC")).toLocalDate();

    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248877",
        currentDate.minusMonths(1).getMonthValue(),
        currentDate.getYear(),
        "089",
        CurrencyCode.EUR,
        new BigDecimal("15.80")
    );

    assertThatThrownBy(() -> PaymentGatewayEndpointValidation.validate(paymentRequest))
        .isInstanceOf(PaymentGatewayValidationException.class)
        .hasMessageContaining("Card has expired");
  }

}