package com.checkout.payment.gateway.exception;


import com.checkout.payment.gateway.dto.PaymentStatus;

public class RejectedPaymentMessage {

  private final PaymentStatus paymentStatus;
  private final ErrorMessage message;

  public RejectedPaymentMessage(ErrorMessage message) {
    this.paymentStatus = PaymentStatus.REJECTED;
    this.message = message;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public ErrorMessage getMessage() {
    return message;
  }

}