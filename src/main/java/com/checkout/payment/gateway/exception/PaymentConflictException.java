package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PaymentConflictException extends ResponseStatusException {

  public PaymentConflictException(String reason) {
    super(HttpStatus.CONFLICT, reason);
  }

}