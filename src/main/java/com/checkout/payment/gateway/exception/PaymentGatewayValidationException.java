package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PaymentGatewayValidationException extends ResponseStatusException {

  public PaymentGatewayValidationException(String reason) {
    super(HttpStatus.BAD_REQUEST, reason);
  }

}