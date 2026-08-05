package com.payment.server.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.payment.server.model.Payment;
import com.payment.server.model.User;
import com.payment.server.repository.BankAccountRepository;
import com.payment.server.repository.CustomerRepository;
import com.payment.server.repository.PaymentRepository;
import com.payment.server.repository.UserRepository;

@Service
public class PaymentValidationService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD");
    private static final Set<String> SUPPORTED_BANKS = Set.of(
            "HDFC Bank", "ICICI Bank", "State Bank of India", "Axis Bank",
            "Kotak Mahindra Bank", "Punjab National Bank", "Bank of Baroda", "Yes Bank");

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final BankAccountRepository bankAccountRepository;

    public PaymentValidationService(CustomerRepository customerRepository, UserRepository userRepository,
            PaymentRepository paymentRepository, BankAccountRepository bankAccountRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.bankAccountRepository = bankAccountRepository;
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
            boolean sourceExists = customerRepository.findByAccountNumber(payment.getSourceAccount()) != null
                    || bankAccountRepository.findByAccountNumber(payment.getSourceAccount()) != null;
            boolean destinationExists = customerRepository.findByAccountNumber(payment.getDestinationAccount()) != null
                    || bankAccountRepository.findByAccountNumber(payment.getDestinationAccount()) != null;
            if (!sourceExists) {
                errors.add("Source account is invalid or doesn't exist");
            }
            if (!destinationExists) {
                errors.add("Destination account is invalid or doesn't exist");
            }

            // Sufficient-funds check - only enforced when the source resolves
            // to a v2 BankAccount (legacy Customer-only accounts have no
            // tracked balance). Prevents a payment from overdrawing the
            // payer's account.
            if (sourceExists && payment.getAmount() != null) {
                com.payment.server.model.BankAccount sourceBankAccount = bankAccountRepository
                        .findByAccountNumber(payment.getSourceAccount());
                if (sourceBankAccount != null
                        && sourceBankAccount.getBalance().compareTo(payment.getAmount()) < 0) {
                    errors.add("Insufficient balance in source account");
                }
            }
        }

        // Customer-specific daily spend limit - see payment-system-v2-design.md
        // section 9. Only enforced when the payment is linked to a platform
        // User with a configured dailyLimit (legacy Customer-only payments skip
        // this check).
        if (payment.getPayerUserId() != null && payment.getAmount() != null) {
            User payer = userRepository.findById(payment.getPayerUserId());
            if (payer != null && payer.getDailyLimit() != null) {
                java.time.LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
                BigDecimal spentToday = paymentRepository.sumAmountByPayerUserIdSince(payer.getId(), startOfToday);
                if (spentToday.add(payment.getAmount()).compareTo(payer.getDailyLimit()) > 0) {
                    errors.add("Payment would exceed daily spending limit of " + payer.getDailyLimit());
                }
            }
        }

        // Credit card validation
        if ("CREDIT_CARD".equals(payment.getPaymentMethod())) {
            String cardNumber = payment.getCardNumber();
            if (cardNumber == null || cardNumber.isBlank()) {
                errors.add("Card number is required for credit card payments");
            } else if (!isValidLuhn(cardNumber)) {
                errors.add("Card number is invalid");
            }

            if (payment.getCardHolderName() == null || payment.getCardHolderName().isBlank()) {
                errors.add("Card holder name is required for credit card payments");
            }

            String expiry = payment.getCardExpiry();
            if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/[0-9]{4}")) {
                errors.add("Card expiry must be in MM/YYYY format");
            } else {
                String[] parts = expiry.split("/");
                YearMonth cardExpiryMonth = YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                if (cardExpiryMonth.isBefore(YearMonth.now())) {
                    errors.add("Card has expired");
                }
            }
        }

        // UPI validation
        if ("UPI".equals(payment.getPaymentMethod())) {
            String upiId = payment.getUpiId();
            if (upiId == null || upiId.isBlank()) {
                errors.add("UPI ID is required for UPI payments");
            } else if (!upiId.matches("^[\\w.\\-]{2,256}@[A-Za-z]{2,64}$")) {
                errors.add("UPI ID must be in the format name@bank");
            }
        }

        // Net banking validation
        if ("NETBANKING".equals(payment.getPaymentMethod())) {
            String bankName = payment.getBankName();
            if (bankName == null || bankName.isBlank()) {
                errors.add("Bank name is required for net banking payments");
            } else if (!SUPPORTED_BANKS.contains(bankName)) {
                errors.add("Bank name is not supported");
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private static boolean isValidLuhn(String cardNumber) {
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 12 || digits.length() > 19) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
