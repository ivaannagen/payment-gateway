package com.checkout.payment.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JweServiceTest {

  @Mock
  private CertificateVault certificateVault;

  @InjectMocks
  private JweService underTest;

  private static final String SECRET_KEY = "VK8VTTPcSgVE8lIpGoE7XOOmsACPpNaH4JHemRoD4IB";

  @Test
  void shouldEncryptAndDecryptObject() throws JsonProcessingException, JOSEException, ParseException {
    CardDetail cardDetail = new CardDetail(
        "7777888899991111",
        "777",
        6,
        2026
    );

    when(certificateVault.getSecretKey()).thenReturn(SECRET_KEY);

    String sourceToken = underTest.encrypt(cardDetail);
    assertThat(sourceToken).isNotBlank();

    CardDetail cardDetails = underTest.decryptToken(sourceToken, CardDetail.class);
    assertThat(cardDetails).satisfies(
        decryptedCardDetail -> {
          assertThat(decryptedCardDetail.getCardNumber()).isEqualTo("7777888899991111");
          assertThat(decryptedCardDetail.getCvv()).isEqualTo("777");
          assertThat(decryptedCardDetail.getExpiryMonth()).isEqualTo(6);
          assertThat(decryptedCardDetail.getExpiryYear()).isEqualTo(2026);
        }
    );

    verify(certificateVault, times(2)).getSecretKey();
  }

}