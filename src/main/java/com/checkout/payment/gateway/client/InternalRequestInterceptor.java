package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.ExceptionMessages;
import com.checkout.payment.gateway.exception.ExternalServiceException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class InternalRequestInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) {
    try {
      request.getHeaders().set("Content-Type", "application/json");
      return execution.execute(request, body);
    } catch (IOException ioe) {
      throw new ExternalServiceException(HttpStatus.FAILED_DEPENDENCY,
          ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE);
    }
  }

}