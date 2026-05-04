package com.checkout.payment.gateway.security;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import java.text.ParseException;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JweService {

  private final CertificateVault certificateVault;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public JweService(CertificateVault certificateVault) {
    this.certificateVault = certificateVault;
  }

  public String encrypt(Object obj) throws JsonProcessingException, JOSEException {
    String jsonPayload = objectMapper.writeValueAsString(obj);
    JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);

    Payload payload = new Payload(jsonPayload);
    JWEObject jweObject = new JWEObject(header, payload);

    jweObject.encrypt(new DirectEncrypter(getSecretKey()));

    return jweObject.serialize();
  }

  public <T> T decryptToken(String jweString, Class<T> clazz)
      throws JsonProcessingException, JOSEException, ParseException {
    JWEObject jweObject = JWEObject.parse(jweString);
    jweObject.decrypt(new DirectDecrypter(getSecretKey()));

    String jsonPayload = jweObject.getPayload().toString();
    return objectMapper.readValue(jsonPayload, clazz);
  }

  /**
   * Secret key would typically be stored in a secure vault for the merchant
   * A dummy secret key has been used for on the fly encryption/decryption to enable the flow.
   */
  private SecretKey getSecretKey() {
    String base64Key = certificateVault.getSecretKey();
    byte[] decodedKey = Base64.getDecoder().decode(base64Key);

    SecretKey aesKey = new SecretKeySpec(decodedKey, "AES");
    return aesKey;
  }

}