package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PaymentIntent;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentIntentRepository {

  private final ConcurrentHashMap<UUID, PaymentIntent> paymentIntents = new ConcurrentHashMap<>();

  public void savePaymentIntent(UUID idempotencyKey, UUID paymentId) {
    paymentIntents.put(idempotencyKey, PaymentIntent.inProgress(paymentId));
  }

  public void updatePaymentIntent(UUID idempotencyKey, PaymentIntent paymentIntent) {
    paymentIntents.put(idempotencyKey, paymentIntent);
  }

  public Optional<PaymentIntent> get(UUID idempotencyKey) {
    return Optional.ofNullable(paymentIntents.get(idempotencyKey));
  }

}