package com.payment.server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.server.dto.CreateBankAccountRequest;
import com.payment.server.dto.CreatePaymentMethodRequest;
import com.payment.server.dto.CreateUserRequest;
import com.payment.server.dto.UpdatePaymentMethodRequest;
import com.payment.server.dto.UpdatePinRequest;
import com.payment.server.model.BankAccount;
import com.payment.server.model.PaymentMethod;
import com.payment.server.model.User;
import com.payment.server.service.UserService;

import jakarta.validation.Valid;

/**
 * Identity & accounts API - register users, link bank accounts, attach
 * payment methods. See payment-system-v2-design.md sections 3 and 8.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/bank-accounts")
    public ResponseEntity<List<BankAccount>> getAllBankAccounts() {
        return ResponseEntity.ok(userService.getAllBankAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable int id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/{id}/bank-accounts")
    public ResponseEntity<List<BankAccount>> getBankAccounts(@PathVariable int id) {
        return ResponseEntity.ok(userService.getBankAccounts(id));
    }

    @GetMapping("/{id}/payment-methods")
    public ResponseEntity<List<PaymentMethod>> getPaymentMethods(@PathVariable int id) {
        return ResponseEntity.ok(userService.getPaymentMethods(id));
    }

    @PostMapping
    public ResponseEntity<User> registerUser(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/bank-accounts")
    public ResponseEntity<BankAccount> addBankAccount(@PathVariable int id,
            @Valid @RequestBody CreateBankAccountRequest request) {
        BankAccount created = userService.addBankAccount(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/payment-methods")
    public ResponseEntity<PaymentMethod> addPaymentMethod(@PathVariable int id,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        PaymentMethod created = userService.addPaymentMethod(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<User> updatePin(@PathVariable int id,
            @Valid @RequestBody UpdatePinRequest request) {
        User updated = userService.updatePin(id, request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/payment-methods/{methodId}")
    public ResponseEntity<PaymentMethod> updatePaymentMethod(@PathVariable int id,
            @PathVariable int methodId, @RequestBody UpdatePaymentMethodRequest request) {
        PaymentMethod updated = userService.updatePaymentMethod(id, methodId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/payment-methods/{methodId}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable int id, @PathVariable int methodId) {
        userService.deletePaymentMethod(id, methodId);
        return ResponseEntity.noContent().build();
    }
}
