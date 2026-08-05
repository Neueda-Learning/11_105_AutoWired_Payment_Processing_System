package com.payment.server.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payment.server.model.Customer;
import com.payment.server.model.Payment;
import com.payment.server.repository.BankAccountRepository;
import com.payment.server.repository.CustomerRepository;
import com.payment.server.repository.PaymentRepository;
import com.payment.server.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentValidationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Test
    void validateReturnsValidForSupportedCurrencyDifferentAccountsAndKnownCustomers() {
        PaymentValidationService service = new PaymentValidationService(customerRepository, userRepository,
                paymentRepository, bankAccountRepository);
        Payment payment = buildPayment("ACC-111", "ACC-222", "usd", "2500.00");
        payment.setUpiId("alice@upi"); // Required for UPI payments

        when(customerRepository.findByAccountNumber("ACC-111")).thenReturn(new Customer("Alice", "ACC-111", "IN"));
        when(customerRepository.findByAccountNumber("ACC-222")).thenReturn(new Customer("Bob", "ACC-222", "IN"));
        when(bankAccountRepository.findByAccountNumber(anyString())).thenReturn(null);

        PaymentValidationService.ValidationResult result = service.validate(payment);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateCollectsAllApplicableErrors() {
        PaymentValidationService service = new PaymentValidationService(customerRepository, userRepository,
                paymentRepository, bankAccountRepository);
        Payment payment = buildPayment("ACC-111", "ACC-111", "zzz", "-5.00");

        when(customerRepository.findByAccountNumber(anyString())).thenReturn(null);
        when(bankAccountRepository.findByAccountNumber(anyString())).thenReturn(null);

        PaymentValidationService.ValidationResult result = service.validate(payment);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Amount must be greater than 0"));
        assertTrue(result.getErrors().contains("Currency is not supported"));
        assertTrue(result.getErrors().contains("Source and destination accounts must be different"));
        assertTrue(result.getErrors().contains("Source account is invalid or doesn't exist"));
        assertTrue(result.getErrors().contains("Destination account is invalid or doesn't exist"));
    }

    @Test
    void validateFailsWhenAccountsMissingWithoutRepositoryLookup() {
        PaymentValidationService service = new PaymentValidationService(customerRepository, userRepository,
                paymentRepository, bankAccountRepository);
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("10.00"));
        payment.setCurrency("USD");

        PaymentValidationService.ValidationResult result = service.validate(payment);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Source and destination accounts are required"));
        verify(customerRepository, never()).findByAccountNumber(anyString());
    }

    private Payment buildPayment(String source, String destination, String currency, String amount) {
        Payment payment = new Payment();
        payment.setSourceAccount(source);
        payment.setDestinationAccount(destination);
        payment.setCurrency(currency);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod("UPI");
        return payment;
    }
}
