package com.checkout.payment.gateway.mapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.dto.BankPaymentRequest;
import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import com.checkout.payment.gateway.exception.PaymentMapperException;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentStatus;
import com.checkout.payment.gateway.security.CardDetail;
import com.checkout.payment.gateway.security.CardDetailsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.neovisionaries.i18n.CurrencyCode;
import com.nimbusds.jose.JOSEException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentMapperTest {

  @Mock
  private CardDetailsService cardDetailsService;

  @InjectMocks
  private PaymentMapper underTest;

  private static final String SOURCE_TOKEN = "XsH3MgeJYxV5abazPXJZ5GBOZXVc5l7gCLMPoVdyZjc";

  @Test
  void shouldMapPaymentRequestSuccessfully() throws JsonProcessingException, JOSEException {
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248877",
        6,
        2026,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    ArgumentCaptor<CardDetail> cardDetailArgumentCaptor = ArgumentCaptor.forClass(CardDetail.class);
    when(cardDetailsService.encryptCardDetails(cardDetailArgumentCaptor.capture())).thenReturn(SOURCE_TOKEN);

    Payment payment = underTest.map(paymentRequest);
    assertThat(payment).satisfies(
        actualPayment -> {
          assertThat(payment.getId()).isNotNull();
          assertThat(payment.getSourceToken()).isNotNull();
          assertThat(payment.getCurrency()).isEqualTo(CurrencyCode.GBP);
          assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15.80"));
          assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
        }
    );
    assertThat(cardDetailArgumentCaptor.getValue()).satisfies(
        cardDetail -> {
          assertThat(cardDetail.getCardNumber()).isEqualTo("2222405343248877");
          assertThat(cardDetail.getCvv()).isEqualTo("089");
          assertThat(cardDetail.getExpiryMonth()).isEqualTo(6);
          assertThat(cardDetail.getExpiryYear()).isEqualTo(2026);
        }
    );
  }

  @Test
  void shouldThrowMapperExceptionWhenUnableToEncryptCardDetails() throws JsonProcessingException, JOSEException {
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248877",
        6,
        2026,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    ArgumentCaptor<CardDetail> cardDetailArgumentCaptor = ArgumentCaptor.forClass(CardDetail.class);
    doThrow(JOSEException.class).when(cardDetailsService).encryptCardDetails(cardDetailArgumentCaptor.capture());

    assertThatThrownBy(() -> underTest.map(paymentRequest))
        .isInstanceOf(PaymentMapperException.class)
        .hasMessageContaining("Unable to map and encrypt card details");
  }

  @Test
  void shouldMapPaymentToBankRequestSuccessfully() throws JsonProcessingException, JOSEException, ParseException {
    Payment payment = new Payment(
        UUID.randomUUID(),
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.IN_PROGRESS
    );
    CardDetail cardDetail = new CardDetail(
        "2222405343248877",
        "089",
        6,
        2026
    );

    when(cardDetailsService.decryptCardDetails(eq(SOURCE_TOKEN))).thenReturn(cardDetail);

    BankPaymentRequest bankPaymentRequest = underTest.map(payment);
    assertThat(bankPaymentRequest).satisfies(
        paymentRequest -> {
          assertThat(paymentRequest.getCardNumber()).isEqualTo("2222405343248877");
          assertThat(paymentRequest.getCvv()).isEqualTo("089");
          assertThat(paymentRequest.getExpiryDate()).isEqualTo("6/2026");
          assertThat(paymentRequest.getCurrency()).isEqualTo(CurrencyCode.GBP);
          assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15.80"));
        }
    );
  }

  @Test
  void shouldMapGetPaymentResponseSuccessfully() throws JsonProcessingException, JOSEException, ParseException {
    UUID paymentId = UUID.randomUUID();
    Payment payment = new Payment(
        paymentId,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.AUTHORIZED
    );
    CardDetail cardDetail = new CardDetail(
        "2222405343248877",
        "089",
        6,
        2026
    );

    when(cardDetailsService.decryptCardDetails(eq(SOURCE_TOKEN))).thenReturn(cardDetail);

    GetPaymentResponse paymentResponse = underTest.mapGetPaymentResponse(payment);
    assertThat(paymentResponse).satisfies(
        response -> {
          assertThat(response.id()).isEqualTo(paymentId);
          assertThat(response.cardNumberLastFour()).isEqualTo("8877");
          assertThat(response.expiryMonth()).isEqualTo(6);
          assertThat(response.expiryYear()).isEqualTo(2026);
          assertThat(response.status()).isEqualTo(com.checkout.payment.gateway.dto.PaymentStatus.AUTHORIZED.getName());
          assertThat(response.currency()).isEqualTo(CurrencyCode.GBP);
          assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15.80"));
        }
    );
  }

  @Test
  void shouldThrowMapperExceptionWhenUnableToDecryptCardDetails() throws JsonProcessingException, JOSEException, ParseException {
    UUID paymentId = UUID.randomUUID();
    Payment payment = new Payment(
        paymentId,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.AUTHORIZED
    );

    doThrow(JsonProcessingException.class).when(cardDetailsService).decryptCardDetails(eq(SOURCE_TOKEN));

    assertThatThrownBy(() -> underTest.mapGetPaymentResponse(payment))
        .isInstanceOf(PaymentMapperException.class)
        .hasMessageContaining("Unable to map and decrypt card details");
  }

  @Test
  void shouldMapPostPaymentResponseSuccessfully() throws JsonProcessingException, JOSEException, ParseException {
    UUID paymentId = UUID.randomUUID();
    Payment payment = new Payment(
        paymentId,
        SOURCE_TOKEN,
        CurrencyCode.GBP,
        new BigDecimal("15.80"),
        PaymentStatus.AUTHORIZED
    );
    CardDetail cardDetail = new CardDetail(
        "2222405343248877",
        "089",
        6,
        2026
    );

    when(cardDetailsService.decryptCardDetails(eq(SOURCE_TOKEN))).thenReturn(cardDetail);

    PostPaymentResponse paymentResponse = underTest.mapPostPaymentResponse(payment);
    assertThat(paymentResponse).satisfies(
        response -> {
          assertThat(response.id()).isEqualTo(paymentId);
          assertThat(response.cardNumberLastFour()).isEqualTo("8877");
          assertThat(response.expiryMonth()).isEqualTo(6);
          assertThat(response.expiryYear()).isEqualTo(2026);
          assertThat(response.status()).isEqualTo(com.checkout.payment.gateway.dto.PaymentStatus.AUTHORIZED.getName());
          assertThat(response.currency()).isEqualTo(CurrencyCode.GBP);
          assertThat(payment.getAmount()).isEqualTo(new BigDecimal("15.80"));
        }
    );
  }

}