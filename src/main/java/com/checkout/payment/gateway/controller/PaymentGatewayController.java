package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.dto.GetPaymentResponse;
import com.checkout.payment.gateway.dto.PostPaymentRequest;
import com.checkout.payment.gateway.dto.PostPaymentResponse;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("payments")
@Validated
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;
  private final PaymentMapper paymentMapper;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService, PaymentMapper paymentMapper) {
    this.paymentGatewayService = paymentGatewayService;
    this.paymentMapper = paymentMapper;
  }

  @GetMapping("{id}")
  public ResponseEntity<GetPaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    GetPaymentResponse paymentResponse = paymentMapper.mapGetPaymentResponse(paymentGatewayService.getPaymentById(id));
    return new ResponseEntity<>(paymentResponse, HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<PostPaymentResponse> createPayment(
      @RequestHeader(value = "Idempotency-Key")
      @NotNull UUID idempotencyKey,
      @Valid @RequestBody PostPaymentRequest postPaymentRequest) {
    PaymentGatewayEndpointValidation.validate(postPaymentRequest);
    Payment payment = paymentGatewayService.processPayment(idempotencyKey,paymentMapper.map(postPaymentRequest));
    return new ResponseEntity<>(paymentMapper.mapPostPaymentResponse(payment), HttpStatus.OK);
  }

}