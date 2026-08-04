package com.payment.server.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.server.dto.LoginRequest;
import com.payment.server.exception.UserNotFoundException;
import com.payment.server.model.Customer;
import com.payment.server.repository.CustomerRepository;

import jakarta.validation.Valid;

/**
 * Lightweight "login" support - no passwords/sessions yet. A user picks their
 * identity (account number) and the server confirms it exists. The returned
 * Customer (id, name, role, own UPI/bank info) is stored client-side and sent
 * back as the X-User-Id header on subsequent requests.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerRepository customerRepository;

    public AuthController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<Customer>> listUsers() {
        return ResponseEntity.ok(customerRepository.findAll());
    }

    @PostMapping("/login")
    public ResponseEntity<Customer> login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerRepository.findByAccountNumber(request.getAccountNumber().trim());
        if (customer == null) {
            throw new UserNotFoundException(request.getAccountNumber());
        }
        return ResponseEntity.ok(customer);
    }
}
