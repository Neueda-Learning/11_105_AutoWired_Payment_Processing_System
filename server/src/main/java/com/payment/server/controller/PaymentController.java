package com.payment.server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payment.server.dto.CreatePaymentRequest;
import com.payment.server.dto.UpdateStatusRequest;
import com.payment.server.exception.UserNotFoundException;
import com.payment.server.model.Customer;
import com.payment.server.model.Payment;
import com.payment.server.model.PaymentStatusHistory;
import com.payment.server.repository.CustomerRepository;
import com.payment.server.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CustomerRepository customerRepository;

    public PaymentController(PaymentService paymentService, CustomerRepository customerRepository) {
        this.paymentService = paymentService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(paymentService.getAllPayments(userId, status));
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
    public ResponseEntity<Payment> createPayment(
            @RequestHeader("X-User-Id") int userId,
            @Valid @RequestBody CreatePaymentRequest request) {
        Customer currentUser = customerRepository.findById(userId);
        if (currentUser == null) {
            throw new UserNotFoundException(String.valueOf(userId));
        }

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setSourceAccount(currentUser.getAccountNumber());
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
            payment.setUpiId(currentUser.getOwnUpiId());
        } else if ("NETBANKING".equals(request.getPaymentMethod())) {
            payment.setBankName(currentUser.getOwnBankName());
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
