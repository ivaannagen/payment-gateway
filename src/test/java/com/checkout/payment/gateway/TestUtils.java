package com.checkout.payment.gateway;

import java.time.LocalDate;

public class TestUtils {

  private TestUtils() {
    throw new IllegalArgumentException("Cannot instantiate static utils");
  }
  public static final Integer VALID_EXPIRY_YEAR = LocalDate.now().plusYears(100).getYear();

}