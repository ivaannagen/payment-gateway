package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.MounteBankResponse;
import com.checkout.payment.gateway.exception.PaymentConflictException;
import com.checkout.payment.gateway.exception.PaymentGatewayNotFoundException;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentIntent;
import com.checkout.payment.gateway.model.PaymentIntentStatus;
import com.checkout.payment.gateway.model.PaymentStatus;
import com.checkout.payment.gateway.repository.PaymentIntentRepository;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private final MounteBankService mounteBankService;

  private final PaymentsRepository paymentsRepository;

  private final PaymentIntentRepository paymentIntentRepository;

  private final PaymentMapper paymentMapper;

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  public PaymentGatewayService(
      MounteBankService mounteBankService,
      PaymentsRepository paymentsRepository,
      PaymentIntentRepository paymentIntentRepository,
      PaymentMapper paymentMapper) {
    this.mounteBankService = mounteBankService;
    this.paymentsRepository = paymentsRepository;
    this.paymentIntentRepository = paymentIntentRepository;
    this.paymentMapper = paymentMapper;
  }

  /**
   * Only return AUTHORIZED and DECLINED payments.
   * No need to return failed or in_progress payments
   */
  public Payment getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID [{}]", id);
    return paymentsRepository.get(id)
        .filter(payment -> Set.of(PaymentStatus.AUTHORIZED, PaymentStatus.DECLINED)
            .contains(payment.getPaymentStatus()))
        .orElseThrow(
            () -> new PaymentGatewayNotFoundException(
                "Payment with ID [%s] cannot be found".formatted(id)));
  }

  public Payment processPayment(UUID idempotencyKey, Payment payment) {
    Optional<PaymentIntent> paymentIntent = paymentIntentRepository.get(idempotencyKey);
    if (paymentIntent.isPresent()) {
      PaymentIntent existingPaymentIntent = paymentIntent.get();
      if (existingPaymentIntent.getPaymentIntentStatus() == PaymentIntentStatus.IN_PROGRESS) {
        throw new PaymentConflictException("Payment request is already being processed!");
      } else {
        UUID existingPaymentId = existingPaymentIntent.getPaymentId();
        Payment existingPayment = paymentsRepository.get(existingPaymentId)
            .orElseThrow(() -> new PaymentGatewayNotFoundException(
                "Unable to find payment with ID [%s]".formatted(existingPaymentId)));
        LOG.info("Payment of terminal status [{}] with ID [{}]", existingPayment.getPaymentStatus(),
            existingPaymentId);
        return existingPayment;
      }
    }

    UUID paymentId = payment.getId();
    createPayment(idempotencyKey, payment);

    try {
      boolean authorized = isPaymentAuthorizedFromBank(payment);
      updatePaymentAuthorization(idempotencyKey, payment, authorized);

      Payment updatedPayment = paymentsRepository.get(paymentId)
          .orElseThrow(() -> new PaymentGatewayNotFoundException(
              "Unable to find payment with ID [%s]".formatted(paymentId)));
      LOG.info("Payment [{}] with ID [{}]", updatedPayment.getPaymentStatus(), paymentId);
      return updatedPayment;

    } catch (Exception e) {
      LOG.info("Payment Failed with ID [{}]", paymentId);
      failPayment(idempotencyKey, payment);
      throw e;
    }

  }

  private boolean isPaymentAuthorizedFromBank(Payment payment) {
    MounteBankResponse mounteBankResponse = mounteBankService.makePayment(
        paymentMapper.map(payment));
    return mounteBankResponse.isAuthorized();
  }

  private void createPayment(UUID idempotencyKey, Payment payment) {
    paymentIntentRepository.savePaymentIntent(idempotencyKey, payment.getId());
    paymentsRepository.savePayment(payment);
  }

  private void updatePaymentAuthorization(UUID idempotencyKey, Payment payment, boolean authorized) {
    paymentsRepository.savePayment(payment.authorized(authorized));
    paymentIntentRepository.updatePaymentIntent(idempotencyKey,
        PaymentIntent.succeeded(payment.getId()));
  }

  private void failPayment(UUID idempotencyKey, Payment payment) {
    paymentIntentRepository.updatePaymentIntent(idempotencyKey,
        PaymentIntent.failed(payment.getId()));
    paymentsRepository.savePayment(payment.failed());
  }

}