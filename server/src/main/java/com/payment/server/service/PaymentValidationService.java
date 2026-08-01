package com.payment.server.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.payment.server.model.Customer;
import com.payment.server.model.Payment;
import com.payment.server.repository.CustomerRepository;

@Service
public class PaymentValidationService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD");

    private final CustomerRepository customerRepository;

    public PaymentValidationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public ValidationResult validate(Payment payment) {
        List<String> errors = new ArrayList<>();

        // Amount validation
        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Amount must be greater than 0");
        } else if (payment.getAmount().compareTo(MAX_AMOUNT) > 0) {
            errors.add("Amount must not exceed " + MAX_AMOUNT);
        } else if (payment.getAmount().scale() > 2) {
            errors.add("Amount must have a maximum of 2 decimal places");
        }

        // Currency validation
        if (payment.getCurrency() == null || !SUPPORTED_CURRENCIES.contains(payment.getCurrency().toUpperCase())) {
            errors.add("Currency is not supported");
        }

        // Account validation
        if (payment.getSourceAccount() == null || payment.getDestinationAccount() == null) {
            errors.add("Source and destination accounts are required");
        } else {
            if (payment.getSourceAccount().equalsIgnoreCase(payment.getDestinationAccount())) {
                errors.add("Source and destination accounts must be different");
            }
            Customer source = customerRepository.findByAccountNumber(payment.getSourceAccount());
            Customer destination = customerRepository.findByAccountNumber(payment.getDestinationAccount());
            if (source == null) {
                errors.add("Source account is invalid or doesn't exist");
            }
            if (destination == null) {
                errors.add("Destination account is invalid or doesn't exist");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}
