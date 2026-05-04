package com.checkout.payment.gateway.dto;

import com.neovisionaries.i18n.CurrencyCode;
import java.io.Serializable;
import java.math.BigDecimal;

public class BankPaymentRequest implements Serializable {

  private final String cardNumber;

  private final String expiryDate;

  private final String cvv;

  private final CurrencyCode currency;

  private final BigDecimal amount;

  public BankPaymentRequest(String cardNumber, String expiryDate, String cvv,
                            CurrencyCode currency, BigDecimal amount) {
    this.cardNumber = cardNumber;
    this.expiryDate = expiryDate;
    this.cvv = cvv;
    this.currency = currency;
    this.amount = amount;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public String getCvv() {
    return cvv;
  }

  public CurrencyCode getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

}