package com.checkout.payment.gateway.service;

import static com.checkout.payment.gateway.TestUtils.VALID_EXPIRY_YEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.MounteBankResponse;
import com.checkout.payment.gateway.client.RestClient;
import com.checkout.payment.gateway.configuration.PaymentGatewayAppConfig;
import com.checkout.payment.gateway.dto.BankPaymentRequest;
import com.neovisionaries.i18n.CurrencyCode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MounteBankServiceTest {

  @Mock
  private RestClient restClient;

  @Mock
  private PaymentGatewayAppConfig paymentGatewayAppConfig;

  @InjectMocks
  private MounteBankService underTest;

  @Test
  void shouldMakePaymentWithMounteBank() {
    final String url = "/payments";
    BankPaymentRequest paymentRequest = new BankPaymentRequest(
        "2222405343248877",
        6 + "/" + VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.EUR,
        new BigDecimal("15.80")
    );

    UUID authorizationCode = UUID.randomUUID();
    MounteBankResponse expectedMountBankResponse = new MounteBankResponse(
        true,
        authorizationCode
    );

    when(paymentGatewayAppConfig.getBankSimulatorUrl()).thenReturn(url);
    when(restClient.postPayment(eq(url), eq(paymentRequest))).thenReturn(expectedMountBankResponse);

    MounteBankResponse mounteBankResponse = underTest.makePayment(paymentRequest);
    assertThat(mounteBankResponse).satisfies(
        bankResponse -> {
          assertThat(bankResponse.isAuthorized()).isTrue();
          assertThat(bankResponse.getAuthorizationCode()).hasValue(authorizationCode);
        }
    );
  }

}