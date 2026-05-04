package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PaymentMapperException extends ResponseStatusException {

  public PaymentMapperException(String reason) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
  }

}