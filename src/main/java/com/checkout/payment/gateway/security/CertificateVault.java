package com.checkout.payment.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * PaymentGateway should not store the customer card details but can
 * be retrieved using token decryption. Can delegate secrets to be stored in secure vault
 * This is yet to be implemented and would likely reside on a server outside of the gateway...
 * Below provider of secret key is for testing
 */
@Service
public class CertificateVault {

  @Value("${certificate.vault.secretkey}")
  private String secretKey;

  public String getSecretKey() {
    return secretKey;
  }

}