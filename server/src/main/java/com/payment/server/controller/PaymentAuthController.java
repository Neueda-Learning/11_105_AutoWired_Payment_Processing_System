package com.payment.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.server.dto.AuthenticatePaymentRequest;
import com.payment.server.dto.InitiatePaymentRequest;
import com.payment.server.model.Payment;
import com.payment.server.service.AuthenticationService;

import jakarta.validation.Valid;

/**
 * PIN/OTP payment authentication gate - starts a payment (triggering an
 * auth challenge) and verifies the payer's PIN/OTP before validation runs.
 * See payment-system-v2-design.md section 5 and 8.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentAuthController {

    private final AuthenticationService authenticationService;

    public PaymentAuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<Payment> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        Payment payment = authenticationService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/{id}/authenticate")
    public ResponseEntity<Payment> authenticate(@PathVariable int id,
            @Valid @RequestBody AuthenticatePaymentRequest request) {
        Payment payment = authenticationService.authenticate(id, request);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/{id}/resend-otp")
    public ResponseEntity<Payment> resendOtp(@PathVariable int id) {
        Payment payment = authenticationService.resendOtp(id);
        return ResponseEntity.ok(payment);
    }
}
