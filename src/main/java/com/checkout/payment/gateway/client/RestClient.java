package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.configuration.RestConfiguration;
import com.checkout.payment.gateway.dto.BankPaymentRequest;
import com.checkout.payment.gateway.exception.ExceptionMessages;
import com.checkout.payment.gateway.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestClient {

  @Autowired
  @Qualifier(value = RestConfiguration.INTERNAL_REST_TEMPLATE)
  private RestTemplate restTemplate;

  public MounteBankResponse postPayment(String url, BankPaymentRequest bankRequest) {
    org.springframework.http.HttpEntity<BankPaymentRequest> httpEntity = new org.springframework.http.HttpEntity<>(
        bankRequest,
        null
    );
    try {
      ResponseEntity<MounteBankResponse> responseEntity = restTemplate.exchange(url,
          HttpMethod.POST, httpEntity, MounteBankResponse.class);
      HttpStatusCode statusCode = responseEntity.getStatusCode();
      if (HttpStatus.OK.equals(statusCode)) {
        return responseEntity.getBody();
      } else {
        throw new ExternalServiceException(HttpStatus.valueOf(statusCode.value()),
            ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE);
      }

    } catch (Exception e) {
      throw new ExternalServiceException(HttpStatus.FAILED_DEPENDENCY, e.getMessage());
    }
  }

}