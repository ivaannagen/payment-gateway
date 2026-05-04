package com.checkout.payment.gateway.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.neovisionaries.i18n.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock
@ActiveProfiles("test")
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String PAYMENT_URL = "/payments";
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
  private static final Integer VALID_EXPIRY_YEAR = LocalDate.now().plusYears(100).getYear();

  @BeforeEach
  void setUp() {
    reset();
  }

  @Test
  void shouldGetPaymentById() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248877",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );
    mockAuthorizedBankResponse();
    MockHttpServletResponse createResponse = postCreatePayment(idempotencyKey,
        paymentRequest).andReturn()
        .getResponse();
    PostPaymentResponse postPaymentResponse = objectMapper.readValue(
        createResponse.getContentAsString(), PostPaymentResponse.class);
    UUID paymentId = postPaymentResponse.id();

    //When
    MockHttpServletResponse getResponse = getPayment(paymentId).andReturn().getResponse();
    GetPaymentResponse getPaymentResponse = objectMapper.readValue(getResponse.getContentAsString(),
        GetPaymentResponse.class);

    //Then
    assertThat(getResponse.getStatus()).isEqualTo(200);
    assertThat(getPaymentResponse).satisfies(
        response -> {
          assertThat(response.id()).isEqualTo(paymentId);
          assertThat(response.status()).isEqualTo("Authorized");
          assertThat(response.cardNumberLastFour()).isEqualTo("8877");
          assertThat(response.expiryMonth()).isEqualTo(6);
          assertThat(response.expiryYear()).isEqualTo(VALID_EXPIRY_YEAR);
          assertThat(response.currency()).isEqualTo(CurrencyCode.GBP);
          assertThat(response.amount()).isEqualTo(new BigDecimal("15.80"));
        }
    );
  }

  @Test
  void shouldThrowNotFoundExceptionWhenPaymentDoesNotExist() throws Exception {
    //Given
    String errorMessage = "Payment with ID [%s] cannot be found".formatted(PAYMENT_ID);

    //When/Then
    getPayment(PAYMENT_ID)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(Matchers.containsString(errorMessage)));
  }

  @Test
  void shouldCreateIdempotentAuthorizedPayment() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248877",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );
    mockAuthorizedBankResponse();

    //When
    MockHttpServletResponse createResponse = postCreatePayment(idempotencyKey,
        paymentRequest).andReturn().getResponse();
    //Idempotent
    MockHttpServletResponse createResponseIdempotent = postCreatePayment(idempotencyKey,
        paymentRequest).andReturn().getResponse();

    PostPaymentResponse postPaymentResponse = objectMapper.readValue(
        createResponseIdempotent.getContentAsString(), PostPaymentResponse.class);
    UUID paymentId = postPaymentResponse.id();

    //Then
    assertThat(createResponse.getStatus()).isEqualTo(200);
    assertThat(postPaymentResponse).satisfies(
        response -> {
          assertThat(response.id()).isEqualTo(paymentId);
          assertThat(response.status()).isEqualTo("Authorized");
          assertThat(response.cardNumberLastFour()).isEqualTo("8877");
          assertThat(response.expiryMonth()).isEqualTo(6);
          assertThat(response.expiryYear()).isEqualTo(VALID_EXPIRY_YEAR);
          assertThat(response.currency()).isEqualTo(CurrencyCode.GBP);
          assertThat(response.amount()).isEqualTo(new BigDecimal("15.80"));
        }
    );
  }

  @Test
  void shouldCreateDeclinedPayment() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248878",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );
    mockDeclinedBankResponse();

    //When
    MockHttpServletResponse createResponse = postCreatePayment(idempotencyKey,
        paymentRequest).andReturn()
        .getResponse();
    PostPaymentResponse postPaymentResponse = objectMapper.readValue(
        createResponse.getContentAsString(), PostPaymentResponse.class);
    UUID paymentId = postPaymentResponse.id();

    //Then
    assertThat(createResponse.getStatus()).isEqualTo(200);
    assertThat(postPaymentResponse).satisfies(
        response -> {
          assertThat(response.id()).isEqualTo(paymentId);
          assertThat(response.status()).isEqualTo("Declined");
          assertThat(response.cardNumberLastFour()).isEqualTo("8878");
          assertThat(response.expiryMonth()).isEqualTo(6);
          assertThat(response.expiryYear()).isEqualTo(VALID_EXPIRY_YEAR);
          assertThat(response.currency()).isEqualTo(CurrencyCode.GBP);
          assertThat(response.amount()).isEqualTo(new BigDecimal("15.80"));
        }
    );
  }

  @Test
  void shouldRejectWhenCardNumberMissing() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        " ",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenCardNumberLessThan14() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899991",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenCardNumberMoreThan19() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "77778888999900001892",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenExpiryMonthMissing() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        null,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @ParameterizedTest
  @MethodSource("expiryMonthFailures")
  void shouldRejectWhenExpiryMonthOutOfBounds(Integer expiryMonth) throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        expiryMonth,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.GBP,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenExpiryYearMissing() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        6,
        null,
        "089",
        CurrencyCode.JPY,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenCurrencyCodeMissing() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        null,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenCurrencyCodeNotSupported() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.EUR,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldThrowWhenCurrencyCodeMissing() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        null,
        new BigDecimal("15.80")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenAmountMissing() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.USD,
        null
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectWhenAmountIsInvalid() throws Exception {
    //Given
    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        6,
        VALID_EXPIRY_YEAR,
        "089",
        CurrencyCode.USD,
        BigDecimal.ZERO
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldRejectPaymentWhenExpiryDateHasPassed() throws Exception {
    //Given
    LocalDate currentDate = Instant.now().atZone(ZoneId.of("UTC")).toLocalDate();

    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "7777888899990022",
        currentDate.minusMonths(1).getMonthValue(),
        currentDate.getYear(),
        "089",
        CurrencyCode.USD,
        new BigDecimal("100")
    );

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.paymentStatus").value("Rejected"))
        .andExpect(jsonPath("$.message").value(Matchers.notNullValue()));
  }

  @Test
  void shouldAcceptPaymentWhenExpiryDateIsCurrentMonth() throws Exception {
    //Given
    LocalDate currentDate = Instant.now().atZone(ZoneId.of("UTC")).toLocalDate();

    UUID idempotencyKey = UUID.randomUUID();
    PostPaymentRequest paymentRequest = new PostPaymentRequest(
        "2222405343248877",
        currentDate.getMonthValue(),
        currentDate.getYear(),
        "089",
        CurrencyCode.USD,
        new BigDecimal("10.50")
    );
    mockAuthorizedBankResponse();

    //When/Then
    postCreatePayment(idempotencyKey, paymentRequest)
        .andExpect(status().isOk());
  }

  private ResultActions postCreatePayment(UUID idempotencyKey, PostPaymentRequest paymentRequest)
      throws Exception {
    return mvc.perform(post(PAYMENT_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header(IDEMPOTENCY_HEADER, idempotencyKey)
        .content(objectMapper.writeValueAsString(paymentRequest)));
  }

  private ResultActions getPayment(UUID paymentId) throws Exception {
    return mvc.perform(MockMvcRequestBuilders.get(PAYMENT_URL + "/" + paymentId));
  }

  private void mockAuthorizedBankResponse() {
    stubFor(WireMock.post(urlEqualTo(PAYMENT_URL))
        .withHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON_VALUE))
        .withRequestBody(matchingJsonPath("$.card_number", equalTo("2222405343248877")))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(
                "{\"authorized\": \"true\", \"authorization_code\": \"dbc34109-e2a4-44cc-a640-f86fd5ad7c3b\"}")));
  }

  private void mockDeclinedBankResponse() {
    stubFor(WireMock.post(urlEqualTo(PAYMENT_URL))
        .withHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON_VALUE))
        .withRequestBody(matchingJsonPath("$.card_number", equalTo("2222405343248878")))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(
                "{\"authorized\": \"false\", \"authorization_code\": \"\"}")));
  }

  private static Stream<Arguments> expiryMonthFailures() {
    return Stream.of(
        Arguments.of(
            0,
            13
        )
    );
  }

}