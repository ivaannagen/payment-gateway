package com.checkout.payment.gateway.service;

import static com.checkout.payment.gateway.TestUtils.VALID_EXPIRY_YEAR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.MounteBankResponse;
import com.checkout.payment.gateway.dto.BankPaymentRequest;
import com.checkout.payment.gateway.exception.ExceptionMessages;
import com.checkout.payment.gateway.exception.ExternalServiceException;
import com.checkout.payment.gateway.exception.PaymentConflictException;
import com.checkout.payment.gateway.exception.PaymentGatewayNotFoundException;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentIntent;
import com.checkout.payment.gateway.model.PaymentIntentStatus;
import com.checkout.payment.gateway.model.PaymentStatus;
import com.checkout.payment.gateway.repository.PaymentIntentRepository;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.neovisionaries.i18n.CurrencyCode;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private MounteBankService mounteBankService;

  @Mock
  private PaymentIntentRepository paymentIntentRepository;

  @Mock
  private PaymentMapper paymentMapper;

  @Mock
  private PaymentsRepository paymentsRepository;

  @InjectMocks
  private PaymentGatewayService underTest;

  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final String SOURCE_TOKEN = "jH1ouLYVUWL8qJ3o2QthI5dcyVY99980go9mZEGvBr1";
  private static final UUID IDEMPOTENCY_KEY = UUID.fromString(
      "5921f572-6cb2-43fe-90e8-cd33b4dd8742");

  @Test
  void shouldGetPaymentSuccessfully() {
    Payment expectedPayment = new Payment(
        PAYMENT_ID,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.AUTHORIZED
    );

    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.of(expectedPayment));

    Payment payment = underTest.getPaymentById(PAYMENT_ID);
    assertThat(payment).satisfies(
        actualPayment -> {
          assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
          assertThat(payment.getSourceToken()).isEqualTo(SOURCE_TOKEN);
          assertThat(payment.getCurrency()).isEqualTo(CurrencyCode.GBP);
          assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15.80"));
          assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        }
    );
  }

  @Test
  void shouldThrowWhenPaymentExistsButNotAuthorizedOrDeclined() {
    Payment expectedPayment = new Payment(
        PAYMENT_ID,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.FAILED
    );

    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.of(expectedPayment));

    assertThatThrownBy(() -> underTest.getPaymentById(PAYMENT_ID))
        .isInstanceOf(PaymentGatewayNotFoundException.class)
        .hasMessageContaining("Payment with ID [%s] cannot be found".formatted(PAYMENT_ID));
  }

  @Test
  void shouldThrowWhenPaymentDoesNotExist() {
    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getPaymentById(PAYMENT_ID))
        .isInstanceOf(PaymentGatewayNotFoundException.class)
        .hasMessageContaining("Payment with ID [%s] cannot be found".formatted(PAYMENT_ID));
  }

  @Test
  void shouldThrowExceptionWhenPaymentIntentInProgress() {
    Payment payment = new Payment(
        PAYMENT_ID,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.IN_PROGRESS
    );

    when(paymentIntentRepository.get(IDEMPOTENCY_KEY)).thenReturn(Optional.of(
        new PaymentIntent(
            PAYMENT_ID,
            PaymentIntentStatus.IN_PROGRESS
        )
    ));

    assertThatThrownBy(() -> underTest.processPayment(IDEMPOTENCY_KEY, payment))
        .isInstanceOf(PaymentConflictException.class)
        .hasMessageContaining("Payment request is already being processed!");

    verify(paymentIntentRepository).get(IDEMPOTENCY_KEY);
    verifyNoInteractions(paymentsRepository);
  }

  @Test
  void shouldReturnPaymentForIdempotencyIfAlreadyProcessed() {
    Payment expectedPayment = new Payment(
        PAYMENT_ID,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.AUTHORIZED
    );

    when(paymentIntentRepository.get(IDEMPOTENCY_KEY)).thenReturn(Optional.of(
        new PaymentIntent(
            PAYMENT_ID,
            PaymentIntentStatus.SUCCEEDED
        )
    ));
    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.of(expectedPayment));

    Payment actualPayment = underTest.processPayment(IDEMPOTENCY_KEY, expectedPayment);
    assertThat(actualPayment).satisfies(
        payment -> {
          assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
          assertThat(payment.getSourceToken()).isEqualTo(SOURCE_TOKEN);
          assertThat(payment.getCurrency()).isEqualTo(CurrencyCode.GBP);
          assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15.80"));
          assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        }
    );

    verify(paymentIntentRepository).get(IDEMPOTENCY_KEY);
    verify(paymentsRepository).get(PAYMENT_ID);
    verifyNoMoreInteractions(paymentIntentRepository, paymentsRepository);
  }

  @Test
  void shouldThrowPaymentIfAlreadyProcessedButMissingFromRepository() {
    Payment expectedPayment = new Payment(
        PAYMENT_ID,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.AUTHORIZED
    );

    when(paymentIntentRepository.get(IDEMPOTENCY_KEY)).thenReturn(Optional.of(
        new PaymentIntent(
            PAYMENT_ID,
            PaymentIntentStatus.SUCCEEDED
        )
    ));
    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.processPayment(IDEMPOTENCY_KEY, expectedPayment))
        .isInstanceOf(PaymentGatewayNotFoundException.class)
        .hasMessageContaining("Unable to find payment with ID [%s]".formatted(PAYMENT_ID));

    verify(paymentIntentRepository).get(IDEMPOTENCY_KEY);
    verify(paymentsRepository).get(PAYMENT_ID);
    verifyNoMoreInteractions(paymentIntentRepository, paymentsRepository);
  }

  @Test
  void shouldReturnNewAuthorizedPayment() {
    Payment expectedPayment = new Payment(
        PAYMENT_ID,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.AUTHORIZED
    );
    BankPaymentRequest paymentRequest = new BankPaymentRequest(
        "2222405343248877",
        6 + "/" + VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    when(paymentIntentRepository.get(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(paymentMapper.map(expectedPayment)).thenReturn(paymentRequest);
    when(mounteBankService.makePayment(paymentRequest)).thenReturn(
        new MounteBankResponse(
            true,
            UUID.randomUUID()
        )
    );

    when(paymentsRepository.get(PAYMENT_ID)).thenReturn(Optional.of(expectedPayment));

    Payment actualPayment = underTest.processPayment(IDEMPOTENCY_KEY, expectedPayment);
    assertThat(actualPayment).satisfies(
        payment -> {
          assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
          assertThat(payment.getSourceToken()).isEqualTo(SOURCE_TOKEN);
          assertThat(payment.getCurrency()).isEqualTo(CurrencyCode.GBP);
          assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15.80"));
          assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        }
    );

    verify(paymentIntentRepository).get(IDEMPOTENCY_KEY);
    verify(paymentIntentRepository).savePaymentIntent(IDEMPOTENCY_KEY, PAYMENT_ID);
    verify(paymentsRepository, times(2)).savePayment(any(Payment.class));
    verify(paymentIntentRepository).updatePaymentIntent(eq(IDEMPOTENCY_KEY),
        any(PaymentIntent.class));
    verify(paymentsRepository).get(PAYMENT_ID);
    verifyNoMoreInteractions(paymentIntentRepository, paymentsRepository);
  }

  @Test
  void shouldThrowWhenPaymentFails() {
    Payment expectedPayment = new Payment(
        PAYMENT_ID,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.FAILED
    );
    BankPaymentRequest paymentRequest = new BankPaymentRequest(
        "2222405343248877",
        6 + "/" + VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    when(paymentIntentRepository.get(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
    when(paymentMapper.map(expectedPayment)).thenReturn(paymentRequest);
    doThrow(new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE,
        ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE)).when(
        mounteBankService).makePayment(paymentRequest);

    assertThatThrownBy(() -> underTest.processPayment(IDEMPOTENCY_KEY, expectedPayment))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageContaining("503 SERVICE_UNAVAILABLE \"Gateway exception with message");

    verify(paymentIntentRepository).get(IDEMPOTENCY_KEY);
    verify(paymentIntentRepository).savePaymentIntent(IDEMPOTENCY_KEY, PAYMENT_ID);
    verify(paymentsRepository, times(2)).savePayment(any(Payment.class));
    verify(paymentIntentRepository).updatePaymentIntent(eq(IDEMPOTENCY_KEY), any(PaymentIntent.class));
    verifyNoMoreInteractions(paymentIntentRepository, paymentsRepository);
  }

}