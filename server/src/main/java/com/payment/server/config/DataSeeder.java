package com.payment.server.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.payment.server.model.BankAccount;
import com.payment.server.model.Customer;
import com.payment.server.model.ErrorCode;
import com.payment.server.model.TransactionFeeRule;
import com.payment.server.model.User;
import com.payment.server.repository.BankAccountRepository;
import com.payment.server.repository.CustomerRepository;
import com.payment.server.repository.ErrorCodeRepository;
import com.payment.server.repository.TransactionFeeRuleRepository;
import com.payment.server.repository.UserRepository;
import com.payment.server.service.OtpHashUtil;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final ErrorCodeRepository errorCodeRepository;
    private final TransactionFeeRuleRepository feeRuleRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    public DataSeeder(CustomerRepository customerRepository, ErrorCodeRepository errorCodeRepository,
            TransactionFeeRuleRepository feeRuleRepository, UserRepository userRepository,
            BankAccountRepository bankAccountRepository) {
        this.customerRepository = customerRepository;
        this.errorCodeRepository = errorCodeRepository;
        this.feeRuleRepository = feeRuleRepository;
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    public void run(String... args) {
        seedCustomers();
        seedErrorCodes();
        seedFeeRules();
        seedUsersAndBankAccounts();
    }

    private void seedCustomers() {
        if (customerRepository.count() > 0) {
            return;
        }
        customerRepository.save(new Customer("Vinay Patel", "ACC1001", "US"));
        customerRepository.save(new Customer("Priyanshu Bariyar", "ACC1002", "PAK"));
        customerRepository.save(new Customer("Raghav Lathi", "ACC1003", "IN"));
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

    private void seedFeeRules() {
        if (feeRuleRepository.count() > 0) {
            return;
        }
        // UPI: flat, negligible fee (subsidized, like real UPI)
        feeRuleRepository.save(flatRule("UPI", "0", null, "1.00"));

        // Net banking: tiered percentage fee with caps
        feeRuleRepository.save(percentRule("NETBANKING", "0", "10000", "0.5", null, "25.00"));
        feeRuleRepository.save(percentRule("NETBANKING", "10000", null, "0.9", null, "500.00"));

        // Credit card: mirrors real MDR
        feeRuleRepository.save(percentRule("CREDIT_CARD", "0", null, "1.75", null, null));
    }

    private TransactionFeeRule flatRule(String method, String min, String max, String feeValue) {
        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod(method);
        rule.setMinAmount(new BigDecimal(min));
        rule.setMaxAmount(max == null ? null : new BigDecimal(max));
        rule.setFeeType("FLAT");
        rule.setFeeValue(new BigDecimal(feeValue));
        rule.setActive(true);
        return rule;
    }

    private TransactionFeeRule percentRule(String method, String min, String max, String feeValue,
            String minCap, String maxCap) {
        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod(method);
        rule.setMinAmount(new BigDecimal(min));
        rule.setMaxAmount(max == null ? null : new BigDecimal(max));
        rule.setFeeType("PERCENTAGE");
        rule.setFeeValue(new BigDecimal(feeValue));
        rule.setMinFeeCap(minCap == null ? null : new BigDecimal(minCap));
        rule.setMaxFeeCap(maxCap == null ? null : new BigDecimal(maxCap));
        rule.setActive(true);
        return rule;
    }

    // Sample platform Users + BankAccounts linked to the seeded legacy
    // Customer accounts, so the new PIN/OTP-gated /initiate + /authenticate
    // flow (payment-system-v2-design.md section 5) is testable out of the
    // box. PIN is "1234" for every seeded user, for demo/testing purposes.
    private void seedUsersAndBankAccounts() {
        if (userRepository.count() > 0) {
            return;
        }
        seedUserWithAccount("Vinay Patel", "vinay@example.com", "ACC1001", "US");
        seedUserWithAccount("Priyanshu Bariyar", "priyanshu@example.com", "ACC1002", "PAK");
        seedUserWithAccount("Raghav Lathi", "raghav@example.com", "ACC1003", "IN");
    }

    private void seedUserWithAccount(String fullName, String email, String accountNumber, String country) {
        User user = new User(fullName, email, null);
        user.setPinHash(OtpHashUtil.hash("1234"));
        user.setKycStatus(User.KYC_VERIFIED);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        BankAccount account = new BankAccount();
        account.setUserId(user.getId());
        account.setAccountNumber(accountNumber);
        account.setBankName("Bank of " + country);
        account.setBalance(new BigDecimal("300000.00"));
        account.setPrimary(true);
        account.setStatus(BankAccount.STATUS_ACTIVE);
        bankAccountRepository.save(account);
    }
}
