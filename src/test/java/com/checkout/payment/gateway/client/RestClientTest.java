package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.dto.BankPaymentRequest;
import com.checkout.payment.gateway.exception.ExternalServiceException;
import com.neovisionaries.i18n.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.UUID;

import static com.checkout.payment.gateway.TestUtils.VALID_EXPIRY_YEAR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestClientTest {


  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private RestClient restClient;

  @Test
  void shouldSuccessfullyUnwrapResponseFromRestClient() {
    String url = "/payments";
    BankPaymentRequest paymentRequest = new BankPaymentRequest(
        "2222405343248877",
        6 + "/" + VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    UUID authorizationCode = UUID.randomUUID();
    MounteBankResponse expectedResponse = new MounteBankResponse(
        true,
        authorizationCode
    );

    ArgumentCaptor<HttpEntity<BankPaymentRequest>> httpEntityArgumentCaptor = ArgumentCaptor.forClass(
        HttpEntity.class);
    when(restTemplate.exchange(eq(url), eq(HttpMethod.POST), httpEntityArgumentCaptor.capture(),
        eq(MounteBankResponse.class)))
        .thenReturn(ResponseEntity.ok(expectedResponse));

    MounteBankResponse mounteBankResponse = restClient.postPayment(url, paymentRequest);
    assertThat(mounteBankResponse).satisfies(
        response -> {
          assertThat(response.isAuthorized()).isTrue();
          assertThat(response.getAuthorizationCode()).hasValue(authorizationCode);
        }
    );

    HttpEntity<BankPaymentRequest> httpEntity = httpEntityArgumentCaptor.getValue();
    assertThat(httpEntity.getBody()).isEqualTo(paymentRequest);
  }

  @Test
  void shouldThrowExceptionFromRestClientWhenErrorCodeNotOk() {
    String url = "/payments";
    BankPaymentRequest paymentRequest = new BankPaymentRequest(
        "2222405343248877",
        6 + "/" + VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    ArgumentCaptor<HttpEntity<BankPaymentRequest>> httpEntityArgumentCaptor = ArgumentCaptor.forClass(
        HttpEntity.class);
    when(restTemplate.exchange(eq(url), eq(HttpMethod.POST), httpEntityArgumentCaptor.capture(),
        eq(MounteBankResponse.class)))
        .thenReturn(ResponseEntity.badRequest().build());

    assertThatThrownBy(() -> restClient.postPayment(url, paymentRequest))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageContaining("400 BAD_REQUEST");
  }

  @Test
  void shouldThrowExceptionFromRestClientWhenUnableToReachDownstream() {
    String url = "/payments";
    BankPaymentRequest paymentRequest = new BankPaymentRequest(
        "2222405343248877",
        6 + "/" + VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    ArgumentCaptor<HttpEntity<BankPaymentRequest>> httpEntityArgumentCaptor = ArgumentCaptor.forClass(
        HttpEntity.class);

    doThrow(new RuntimeException()).when(restTemplate).exchange(eq(url), eq(HttpMethod.POST), httpEntityArgumentCaptor.capture(),
        eq(MounteBankResponse.class));

    assertThatThrownBy(() -> restClient.postPayment(url, paymentRequest))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageContaining("424 FAILED_DEPENDENCY");
  }

}