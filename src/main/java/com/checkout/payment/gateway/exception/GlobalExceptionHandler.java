package com.checkout.payment.gateway.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(value = ConstraintViolationException.class)
  public ResponseEntity<RejectedPaymentMessage> handleConstraintViolationException(
      HttpServletRequest request, HttpServletResponse response, ConstraintViolationException cve) {
    return ResponseEntity.status(response.getStatus())
        .body(new RejectedPaymentMessage(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE,
                cve.getConstraintViolations().stream().map(
                    ConstraintViolation::getMessage).collect(Collectors.toList())))));

  }

  @ExceptionHandler(value = PaymentGatewayValidationException.class)
  public ResponseEntity<RejectedPaymentMessage> handlePaymentGatewayValidationException(HttpServletRequest request,
      PaymentGatewayValidationException pge) {
    return ResponseEntity.status(pge.getStatusCode())
        .body(new RejectedPaymentMessage(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, pge.getReason()))));

  }

  @ExceptionHandler(value = PaymentConflictException.class)
  public ResponseEntity<ErrorMessage> handlePaymentConflictException(HttpServletRequest request,
      PaymentConflictException pce) {
    return ResponseEntity.status(pce.getStatusCode())
        .body(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, pce.getReason())));

  }

  @ExceptionHandler(value = PaymentMapperException.class)
  public ResponseEntity<ErrorMessage> handlePaymentMapperException(HttpServletRequest request,
      PaymentMapperException pme) {
    return ResponseEntity.status(pme.getStatusCode())
        .body(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, pme.getReason())));

  }

  @ExceptionHandler(value = PaymentGatewayNotFoundException.class)
  public ResponseEntity<ErrorMessage> handlePaymentGatewayNotFoundException(HttpServletRequest request,
      PaymentGatewayNotFoundException pgnfe) {
    return ResponseEntity.status(pgnfe.getStatusCode())
        .body(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, pgnfe.getReason())));

  }

  @ExceptionHandler(value = ExternalServiceException.class)
  public ResponseEntity<ErrorMessage> handleExternalServiceException(HttpServletRequest request, ExternalServiceException ese) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, ese.getReason())));

  }

  @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
  public ResponseEntity<RejectedPaymentMessage> handleMethodArgumentTypeMismatchException(HttpServletRequest request, HttpServletResponse response, MethodArgumentTypeMismatchException matme) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new RejectedPaymentMessage(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, matme.getMessage() + matme.getParameter()))));

  }

  @ExceptionHandler(value = MethodArgumentNotValidException.class)
  public ResponseEntity<RejectedPaymentMessage> handleMethodArgumentNotValidException(HttpServletRequest request, HttpServletResponse response, MethodArgumentNotValidException manve) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new RejectedPaymentMessage(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, manve.getMessage() + manve.getParameter()))));

  }

  @ExceptionHandler(value = MissingRequestHeaderException.class)
  public ResponseEntity<RejectedPaymentMessage> handleMissingRequestHeaderException(HttpServletRequest request, HttpServletResponse response, MissingRequestHeaderException mrhe) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new RejectedPaymentMessage(getErrorMessageForException(request.getRequestURI(),
            String.format(ExceptionMessages.GATEWAY_EXCEPTION_MESSAGE, mrhe.getMessage() + mrhe.getParameter()))));

  }

  private ErrorMessage getErrorMessageForException(String uri, String message) {
    ErrorMessage errorMessage = new com.checkout.payment.gateway.exception.ErrorMessage(
        System.currentTimeMillis(),
        message
    );

    LOG.error("Exception occurred for URI: [{}] with message detail [{}]", uri, message);
    return errorMessage;
  }

}