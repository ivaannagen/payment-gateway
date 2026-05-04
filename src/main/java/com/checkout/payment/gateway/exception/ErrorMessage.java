package com.checkout.payment.gateway.exception;

public class ErrorMessage {

  private final long timestamp;
  private final String message;

  public ErrorMessage(long timestamp, String message) {
    this.timestamp = timestamp;
    this.message = message;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getMessage() {
    return message;
  }

}