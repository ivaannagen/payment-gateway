package com.checkout.payment.gateway.security;

public class CardDetail {

  private String cardNumber;

  private String cvv;

  private Integer expiryMonth;

  private Integer expiryYear;

  public CardDetail(String cardNumber, String cvv, Integer expiryMonth, Integer expiryYear) {
    this.cardNumber = cardNumber;
    this.cvv = cvv;
    this.expiryMonth = expiryMonth;
    this.expiryYear = expiryYear;
  }

  public CardDetail(){}

  public String getCardNumber() {
    return cardNumber;
  }

  public String getCvv() {
    return cvv;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

}