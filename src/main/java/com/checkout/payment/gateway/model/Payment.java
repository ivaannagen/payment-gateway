package com.checkout.payment.gateway.model;

import com.neovisionaries.i18n.CurrencyCode;
import java.math.BigDecimal;
import java.util.UUID;

public class Payment {

  private final UUID id;
  private final String sourceToken;
  private final CurrencyCode currency;
  private final BigDecimal amount;
  private final PaymentStatus paymentStatus;

  public Payment(
      UUID id,
      String sourceToken,
      CurrencyCode currency,
      BigDecimal amount,
      PaymentStatus paymentStatus) {
    this.id = id;
    this.sourceToken = sourceToken;
    this.currency = currency;
    this.amount = amount;
    this.paymentStatus = paymentStatus;
  }

  public UUID getId() {
    return id;
  }

  public String getSourceToken() {
    return sourceToken;
  }

  public CurrencyCode getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public Payment authorized(boolean authorized) {
    return new Payment(
        id,
        sourceToken,
        currency,
        amount,
        authorized ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED
    );
  }

  public Payment failed() {
    return new Payment(
        id,
        sourceToken,
        currency,
        amount,
        PaymentStatus.FAILED
    );
  }

}