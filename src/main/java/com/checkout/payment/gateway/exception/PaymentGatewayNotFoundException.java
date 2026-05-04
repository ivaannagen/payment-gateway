package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PaymentGatewayNotFoundException extends ResponseStatusException {

  public PaymentGatewayNotFoundException(String reason) {
    super(HttpStatus.NOT_FOUND, reason);
  }

}