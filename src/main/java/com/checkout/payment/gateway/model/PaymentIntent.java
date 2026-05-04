package com.checkout.payment.gateway.model;

import java.util.UUID;

public class PaymentIntent {

  private final UUID paymentId;
  private final PaymentIntentStatus paymentIntentStatus;

  public PaymentIntent(UUID paymentId, PaymentIntentStatus paymentIntentStatus) {
    this.paymentId = paymentId;
    this.paymentIntentStatus = paymentIntentStatus;
  }

  public static PaymentIntent inProgress(UUID paymentId) {
    return new PaymentIntent(paymentId, PaymentIntentStatus.IN_PROGRESS);
  }

  public static PaymentIntent succeeded(UUID paymentId) {
    return new PaymentIntent(paymentId, PaymentIntentStatus.SUCCEEDED);
  }

  public static PaymentIntent failed(UUID paymentId) {
    return new PaymentIntent(paymentId, PaymentIntentStatus.FAILED);
  }

  public UUID getPaymentId() {
    return paymentId;
  }

  public PaymentIntentStatus getPaymentIntentStatus() {
    return paymentIntentStatus;
  }

}