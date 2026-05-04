package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.RestClient;
import com.checkout.payment.gateway.configuration.PaymentGatewayAppConfig;
import com.checkout.payment.gateway.dto.BankPaymentRequest;
import com.checkout.payment.gateway.client.MounteBankResponse;
import org.springframework.stereotype.Service;

@Service
public class MounteBankService implements BankService<MounteBankResponse> {

  private final RestClient restClient;
  private final PaymentGatewayAppConfig paymentGatewayAppConfig;

  public MounteBankService(RestClient restClient, PaymentGatewayAppConfig paymentGatewayAppConfig) {
    this.restClient = restClient;
    this.paymentGatewayAppConfig = paymentGatewayAppConfig;
  }

  public MounteBankResponse makePayment(BankPaymentRequest paymentRequest) {
    return restClient.postPayment(
        paymentGatewayAppConfig.getBankSimulatorUrl(),
        paymentRequest
    );
  }

}