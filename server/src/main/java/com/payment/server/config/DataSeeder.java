package com.payment.server.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.payment.server.model.Customer;
import com.payment.server.model.ErrorCode;
import com.payment.server.repository.CustomerRepository;
import com.payment.server.repository.ErrorCodeRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final ErrorCodeRepository errorCodeRepository;

    public DataSeeder(CustomerRepository customerRepository, ErrorCodeRepository errorCodeRepository) {
        this.customerRepository = customerRepository;
        this.errorCodeRepository = errorCodeRepository;
    }

    @Override
    public void run(String... args) {
        seedCustomers();
        seedErrorCodes();
    }

    private void seedCustomers() {
        if (customerRepository.count() > 0) {
            return;
        }
        customerRepository.save(new Customer("Alice Johnson", "ACC1001", "US", "USER",
                "alice@hdfcbank", "HDFC Bank"));
        customerRepository.save(new Customer("Bob Smith", "ACC1002", "US", "USER",
                "bob@icicibank", "ICICI Bank"));
        customerRepository.save(new Customer("Charlie Brown", "ACC1003", "GB", "USER",
                "charlie@axisbank", "Axis Bank"));
        customerRepository.save(new Customer("Diana Prince", "ACC1004", "GB", "USER",
                "diana@sbi", "State Bank of India"));
        customerRepository.save(new Customer("Ethan Hunt", "ACC1005", "IN", "USER",
                "ethan@kotak", "Kotak Mahindra Bank"));
        customerRepository.save(new Customer("Ops Admin", "ACC9001", "US", "ADMIN", null, null));
    }

    private void seedErrorCodes() {
        if (errorCodeRepository.count() > 0) {
            return;
        }
        errorCodeRepository.save(new ErrorCode("VALIDATION_FAILED", "Payment failed validation checks", 400, "ERROR"));
        errorCodeRepository
                .save(new ErrorCode("INSUFFICIENT_FUNDS", "Source account has insufficient funds", 400, "ERROR"));
        errorCodeRepository
                .save(new ErrorCode("INVALID_ACCOUNT", "Account number is invalid or doesn't exist", 400, "ERROR"));
        errorCodeRepository.save(new ErrorCode("INVALID_CURRENCY", "Currency code is not supported", 400, "ERROR"));
        errorCodeRepository.save(new ErrorCode("INVALID_AMOUNT", "Amount is zero, negative, or invalid", 400, "ERROR"));
        errorCodeRepository
                .save(new ErrorCode("DUPLICATE_PAYMENT", "Payment with same idempotency key exists", 409, "WARN"));
        errorCodeRepository.save(new ErrorCode("INVALID_STATUS_TRANSITION",
                "Cannot transition from current status to requested status", 400, "ERROR"));
        errorCodeRepository.save(new ErrorCode("PAYMENT_NOT_FOUND", "Payment ID does not exist", 404, "ERROR"));
        errorCodeRepository
                .save(new ErrorCode("PROCESSING_ERROR", "Internal error during payment processing", 500, "ERROR"));
        errorCodeRepository
                .save(new ErrorCode("NETWORK_ERROR", "Communication failure with payment network", 503, "ERROR"));
    }
}
