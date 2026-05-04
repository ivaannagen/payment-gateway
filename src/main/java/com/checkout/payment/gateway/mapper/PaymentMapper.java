package com.checkout.payment.gateway.mapper;

import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.BankPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import com.checkout.payment.gateway.exception.PaymentMapperException;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentStatus;
import com.checkout.payment.gateway.security.CardDetail;
import com.checkout.payment.gateway.security.CardDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class PaymentMapper {

  private final CardDetailsService cardDetailsService;

  private static final Logger LOG = LoggerFactory.getLogger(PaymentMapper.class);

  public PaymentMapper(CardDetailsService cardDetailsService) {
    this.cardDetailsService = cardDetailsService;
  }

  public Payment map(PostPaymentRequest paymentRequest) {
    try {
      String sourceToken = cardDetailsService.encryptCardDetails(
          new CardDetail(
              paymentRequest.cardNumber(),
              paymentRequest.cvv(),
              paymentRequest.expiryMonth(),
              paymentRequest.expiryYear()
          )
      );
      return new Payment(
          UUID.randomUUID(),
          sourceToken,
          paymentRequest.currency(),
          paymentRequest.amount(),
          PaymentStatus.IN_PROGRESS
      );
    } catch (Exception e) {
      LOG.error("Could not encrypt card details successfully");
      throw new PaymentMapperException("Unable to map and encrypt card details");
    }
  }

  public BankPaymentRequest map(Payment payment) {
    try {
      CardDetail cardDetail = cardDetailsService.decryptCardDetails(payment.getSourceToken());
      return new BankPaymentRequest(
          cardDetail.getCardNumber(),
          cardDetail.getExpiryMonth() + "/" + cardDetail.getExpiryYear(),
          cardDetail.getCvv(),
          payment.getCurrency(),
          payment.getAmount()
      );
    } catch (Exception e) {
      LOG.error("Could not decrypt card details successfully");
      throw new PaymentMapperException("Unable to map and decrypt card details");
    }
  }

  public GetPaymentResponse mapGetPaymentResponse(Payment payment) {
    try {
      CardDetail cardDetail = cardDetailsService.decryptCardDetails(payment.getSourceToken());
      return new GetPaymentResponse(
          payment.getId(),
          map(payment.getPaymentStatus()).getName(),
          stripLastFour(cardDetail.getCardNumber()),
          cardDetail.getExpiryMonth(),
          cardDetail.getExpiryYear(),
          payment.getCurrency(),
          payment.getAmount()

      );
    } catch (Exception e) {
      LOG.error("Could not decrypt card details successfully");
      throw new PaymentMapperException("Unable to map and decrypt card details");
    }
  }

  public PostPaymentResponse mapPostPaymentResponse(Payment payment) {
    try {
      CardDetail cardDetail = cardDetailsService.decryptCardDetails(payment.getSourceToken());
      return new PostPaymentResponse(
          payment.getId(),
          map(payment.getPaymentStatus()).getName(),
          stripLastFour(cardDetail.getCardNumber()),
          cardDetail.getExpiryMonth(),
          cardDetail.getExpiryYear(),
          payment.getCurrency(),
          payment.getAmount()
      );
    } catch (Exception e) {
      LOG.error("Could not decrypt card details successfully");
      throw new PaymentMapperException("Unable to map and decrypt card details");
    }
  }

  private String stripLastFour(String cardNumber) {
    int cardNumberLength = cardNumber.length();
    return cardNumber.substring(cardNumberLength - 4, cardNumberLength);
  }

  private com.checkout.payment.gateway.dto.PaymentStatus map(PaymentStatus paymentStatus) {
    return switch (paymentStatus) {
      case AUTHORIZED -> com.checkout.payment.gateway.dto.PaymentStatus.AUTHORIZED;
      case DECLINED -> com.checkout.payment.gateway.dto.PaymentStatus.DECLINED;
      case FAILED -> com.checkout.payment.gateway.dto.PaymentStatus.FAILED;
      case IN_PROGRESS -> com.checkout.payment.gateway.dto.PaymentStatus.IN_PROGRESS;
    };
  }

}