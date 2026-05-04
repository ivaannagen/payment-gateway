package com.checkout.payment.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import java.text.ParseException;
import org.springframework.stereotype.Service;

@Service
public class CardDetailsService {

  private final JweService jweService;

  public CardDetailsService(JweService jweService) {
    this.jweService = jweService;
  }

  public CardDetail decryptCardDetails(String sourceToken)
      throws ParseException, JOSEException, JsonProcessingException {
    final CardDetail cardDetails = jweService.decryptToken(sourceToken, CardDetail.class);
    return cardDetails;
  }

  public String encryptCardDetails(CardDetail cardDetail)
      throws JsonProcessingException, JOSEException {
    final String token = jweService.encrypt(cardDetail);
    return token;
  }

}