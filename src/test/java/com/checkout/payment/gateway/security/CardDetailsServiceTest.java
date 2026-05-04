package com.checkout.payment.gateway.security;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import java.text.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardDetailsServiceTest {

  @Mock
  private JweService jweService;

  @InjectMocks
  private CardDetailsService underTest;

  private static final String SOURCE_TOKEN = "XsH3MgeJYxV5abazPXJZ5GBOZXVc5l7gCLMPoVdyZjc";

  @Test
  void shouldDecryptCardDetails() throws ParseException, JOSEException, JsonProcessingException {
    CardDetail expectedCardDetail = new CardDetail(
        "7777888899991111",
        "777",
        6,
        2026
    );

    when(jweService.decryptToken(SOURCE_TOKEN, CardDetail.class)).thenReturn(expectedCardDetail);

    CardDetail actualCardDetail = underTest.decryptCardDetails(SOURCE_TOKEN);
    assertThat(actualCardDetail).satisfies(
        cardDetail -> {
          assertThat(cardDetail.getCardNumber()).isEqualTo("7777888899991111");
          assertThat(cardDetail.getCvv()).isEqualTo("777");
          assertThat(cardDetail.getExpiryMonth()).isEqualTo(6);
          assertThat(cardDetail.getExpiryYear()).isEqualTo(2026);
        }
    );
  }

  @Test
  void shouldEncryptCardDetails() throws ParseException, JOSEException, JsonProcessingException {
    CardDetail cardDetail = new CardDetail(
        "7777888899991111",
        "777",
        6,
        2026
    );

    when(jweService.encrypt(cardDetail)).thenReturn(SOURCE_TOKEN);

    String sourceToken = underTest.encryptCardDetails(cardDetail);
    assertThat(sourceToken).isEqualTo(SOURCE_TOKEN);
  }

}