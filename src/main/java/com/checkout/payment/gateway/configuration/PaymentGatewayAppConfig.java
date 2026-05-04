package com.checkout.payment.gateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentGatewayAppConfig {

  @Value("${bank.simulator.url}")
  private String bankSimulatorUrl;

  public String getBankSimulatorUrl() {
    return bankSimulatorUrl;
  }

}