package com.payment.server.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payment.server.dto.AdminStatsResponse;
import com.payment.server.exception.ForbiddenException;
import com.payment.server.exception.UserNotFoundException;
import com.payment.server.model.Customer;
import com.payment.server.model.Payment;
import com.payment.server.repository.CustomerRepository;
import com.payment.server.service.PaymentService;

/**
 * Admin-only endpoints: cross-user payment visibility and platform-wide
 * figures (total volume, total payment count, processing fee earnings).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PaymentService paymentService;
    private final CustomerRepository customerRepository;

    public AdminController(PaymentService paymentService, CustomerRepository customerRepository) {
        this.paymentService = paymentService;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments(
            @RequestHeader("X-User-Id") int userId,
            @RequestParam(required = false) String status) {
        requireAdmin(userId);
        return ResponseEntity.ok(paymentService.getAllPayments(null, status));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats(@RequestHeader("X-User-Id") int userId) {
        requireAdmin(userId);
        return ResponseEntity.ok(paymentService.getAdminStats());
    }

    private Customer requireAdmin(int userId) {
        Customer user = customerRepository.findById(userId);
        if (user == null) {
            throw new UserNotFoundException(String.valueOf(userId));
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Admin access required");
        }
        return user;
    }
}
