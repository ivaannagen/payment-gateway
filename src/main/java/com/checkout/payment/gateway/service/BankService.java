package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.dto.BankPaymentRequest;

public interface BankService<R> {

  R makePayment(BankPaymentRequest paymentRequest);

}