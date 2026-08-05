package com.payment.server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payment.server.dto.CreatePaymentRequest;
import com.payment.server.dto.PaymentStats;
import com.payment.server.dto.UpdateStatusRequest;
import com.payment.server.model.Payment;
import com.payment.server.model.PaymentStatusHistory;
import com.payment.server.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(paymentService.getAllPayments(status));
    }

    @GetMapping("/stats")
    public ResponseEntity<PaymentStats> getStats() {
        return ResponseEntity.ok(paymentService.getStats());
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<Payment>> getFlaggedPayments() {
        return ResponseEntity.ok(paymentService.getFlaggedPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable int id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<PaymentStatusHistory>> getPaymentHistory(@PathVariable int id) {
        return ResponseEntity.ok(paymentService.getHistory(id));
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setSourceAccount(request.getSourceAccount());
        payment.setDestinationAccount(request.getDestinationAccount());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReference(request.getReference());
        payment.setIdempotencyKey(request.getIdempotencyKey());

        if ("CREDIT_CARD".equals(request.getPaymentMethod())) {
            payment.setCardNumber(request.getCardNumber());
            payment.setCardHolderName(request.getCardHolderName());
            payment.setCardExpiry(request.getCardExpiry());
            if (request.getCardNumber() != null) {
                String digitsOnly = request.getCardNumber().replaceAll("\\D", "");
                if (digitsOnly.length() >= 4) {
                    payment.setCardLast4(digitsOnly.substring(digitsOnly.length() - 4));
                }
            }
        } else if ("UPI".equals(request.getPaymentMethod())) {
            payment.setUpiId(request.getUpiId());
        } else if ("NETBANKING".equals(request.getPaymentMethod())) {
            payment.setBankName(request.getBankName());
        }

        Payment created = paymentService.createPayment(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Payment> updateStatus(@PathVariable int id,
            @Valid @RequestBody UpdateStatusRequest request) {
        Payment updated = paymentService.transitionStatus(id, request.getStatus(), request.getNotes());
        return ResponseEntity.ok(updated);
    }
}
