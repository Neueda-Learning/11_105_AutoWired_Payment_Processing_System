package com.payment.server.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.payment.server.dto.CreateBankAccountRequest;
import com.payment.server.dto.CreatePaymentMethodRequest;
import com.payment.server.dto.CreateUserRequest;
import com.payment.server.exception.BankAccountNotFoundException;
import com.payment.server.exception.DuplicateUserException;
import com.payment.server.exception.UserNotFoundException;
import com.payment.server.model.BankAccount;
import com.payment.server.model.PaymentMethod;
import com.payment.server.model.User;
import com.payment.server.repository.BankAccountRepository;
import com.payment.server.repository.PaymentMethodRepository;
import com.payment.server.repository.UserRepository;

/**
 * Identity & accounts - registers users, links bank accounts, and attaches
 * payment methods. See payment-system-v2-design.md sections 3 and 8
 * (Sprint A).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public UserService(UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public User registerUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new DuplicateUserException(request.getEmail());
        }

        User user = new User(request.getFullName(), request.getEmail(), request.getPhone());
        // PIN is hashed immediately and never persisted/returned in raw form.
        user.setPinHash(OtpHashUtil.hash(request.getPin()));
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        return user;
    }

    public User getUserById(int id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return user;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public List<BankAccount> getBankAccounts(int userId) {
        getUserById(userId); // ensures user exists, throws if not
        return bankAccountRepository.findByUserId(userId);
    }

    public List<PaymentMethod> getPaymentMethods(int userId) {
        getUserById(userId);
        return paymentMethodRepository.findByUserId(userId);
    }

    public BankAccount addBankAccount(int userId, CreateBankAccountRequest request) {
        getUserById(userId); // ensures user exists, throws if not

        BankAccount account = new BankAccount();
        account.setUserId(userId);
        account.setAccountNumber(request.getAccountNumber());
        account.setIfscCode(request.getIfscCode());
        account.setBankName(request.getBankName());
        account.setBalance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO);
        account.setPrimary(request.isPrimary());
        account.setStatus(BankAccount.STATUS_ACTIVE);

        bankAccountRepository.save(account);
        return account;
    }

    public PaymentMethod addPaymentMethod(int userId, CreatePaymentMethodRequest request) {
        getUserById(userId);

        BankAccount account = bankAccountRepository.findById(request.getBankAccountId());
        if (account == null || account.getUserId() != userId) {
            throw new BankAccountNotFoundException(request.getBankAccountId());
        }

        PaymentMethod method = new PaymentMethod();
        method.setUserId(userId);
        method.setBankAccountId(request.getBankAccountId());
        method.setType(request.getType());
        method.setUpiId(request.getUpiId());
        method.setLinkedBankName(request.getLinkedBankName());
        method.setDefault(request.isDefault());

        if (request.getCardNumber() != null && !request.getCardNumber().isBlank()) {
            String digitsOnly = request.getCardNumber().replaceAll("\\D", "");
            if (digitsOnly.length() >= 4) {
                method.setCardLast4(digitsOnly.substring(digitsOnly.length() - 4));
            }
            // Opaque token only - raw card number is never persisted.
            method.setCardToken(OtpHashUtil.hash(request.getCardNumber()));
        }

        if (request.isDefault()) {
            paymentMethodRepository.clearDefaultForUser(userId);
        }

        paymentMethodRepository.save(method);
        return method;
    }
}
