package com.payment.server.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(PaymentNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Object> handleInvalidTransition(InvalidStatusTransitionException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<Object> handleValidation(PaymentValidationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errorCode", "VALIDATION_FAILED");
        body.put("message", ex.getMessage());
        body.put("errors", ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<Object> handleDuplicatePayment(DuplicatePaymentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("errorCode", "DUPLICATE_PAYMENT");
        body.put("message", ex.getMessage());
        body.put("existingPaymentId", ex.getExistingPaymentId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFound(UserNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<Object> handleDuplicateUser(DuplicateUserException ex) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_USER", ex.getMessage());
    }

    @ExceptionHandler(IncorrectPinException.class)
    public ResponseEntity<Object> handleIncorrectPin(IncorrectPinException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INCORRECT_PIN", ex.getMessage());
    }

    @ExceptionHandler(BankAccountNotFoundException.class)
    public ResponseEntity<Object> handleBankAccountNotFound(BankAccountNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "BANK_ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(PaymentMethodNotFoundException.class)
    public ResponseEntity<Object> handlePaymentMethodNotFound(PaymentMethodNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "PAYMENT_METHOD_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(FeeRuleNotFoundException.class)
    public ResponseEntity<Object> handleFeeRuleNotFound(FeeRuleNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "FEE_RULE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AuthChallengeExpiredException.class)
    public ResponseEntity<Object> handleAuthChallengeExpired(AuthChallengeExpiredException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "AUTH_CHALLENGE_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler(OtpResendLimitExceededException.class)
    public ResponseEntity<Object> handleOtpResendLimitExceeded(OtpResendLimitExceededException ex) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_LIMIT_EXCEEDED", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Object> handleAuthenticationFailed(AuthenticationFailedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("errorCode", "AUTH_FAILED");
        body.put("message", ex.getMessage());
        body.put("paymentId", ex.getPaymentId());
        body.put("attemptsRemaining", ex.getAttemptsRemaining());
        body.put("locked", ex.isLocked());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errorCode", "VALIDATION_FAILED");
        body.put("message", "Request validation failed");
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "PROCESSING_ERROR", ex.getMessage());
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("errorCode", errorCode);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
