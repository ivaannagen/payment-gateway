package com.checkout.payment.gateway.client;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

public class MounteBankResponse implements Serializable {

  private Boolean authorized;

  private UUID authorizationCode;

  public MounteBankResponse(Boolean authorized, UUID authorizationCode) {
    this.authorized = authorized;
    this.authorizationCode = authorizationCode;
  }

  public MounteBankResponse(){}

  public Boolean isAuthorized() {
    return authorized;
  }

  public Optional<UUID> getAuthorizationCode() {
    return Optional.ofNullable(authorizationCode);
  }

}