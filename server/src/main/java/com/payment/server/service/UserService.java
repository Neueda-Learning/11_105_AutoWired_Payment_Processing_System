package com.payment.server.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.payment.server.dto.CreateBankAccountRequest;
import com.payment.server.dto.CreatePaymentMethodRequest;
import com.payment.server.dto.CreateUserRequest;
import com.payment.server.dto.UpdatePaymentMethodRequest;
import com.payment.server.dto.UpdatePinRequest;
import com.payment.server.exception.BankAccountNotFoundException;
import com.payment.server.exception.DuplicateUserException;
import com.payment.server.exception.IncorrectPinException;
import com.payment.server.exception.PaymentMethodNotFoundException;
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

    /**
     * Changes a user's PIN from the customer profile section - requires the
     * current PIN to match before accepting the new one.
     */
    public User updatePin(int userId, UpdatePinRequest request) {
        User user = getUserById(userId);

        if (user.getPinHash() == null
                || !OtpHashUtil.matches(request.getCurrentPin(), user.getPinHash())) {
            throw new IncorrectPinException();
        }

        String newPinHash = OtpHashUtil.hash(request.getNewPin());
        userRepository.updatePinHash(userId, newPinHash);
        user.setPinHash(newPinHash);
        return user;
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

        if (PaymentMethod.TYPE_CARD.equals(request.getType())) {
            validateCardDetails(request.getCardNumber(), request.getCardExpiry(), request.getCardHolderName());

            String digitsOnly = request.getCardNumber().replaceAll("\\D", "");
            method.setCardLast4(digitsOnly.substring(digitsOnly.length() - 4));
            // Opaque token only - raw card number is never persisted.
            method.setCardToken(OtpHashUtil.hash(request.getCardNumber()));
            method.setCardExpiry(request.getCardExpiry());
            method.setCardHolderName(request.getCardHolderName());
        }

        if (request.isDefault()) {
            paymentMethodRepository.clearDefaultForUser(userId);
        }

        paymentMethodRepository.save(method);
        return method;
    }

    /**
     * Validates card details supplied when adding/editing a CARD payment
     * method. The raw card number is only ever seen here (and briefly during
     * legacy raw-entry payments) - it is Luhn-checked once and then
     * discarded, never persisted.
     */
    private void validateCardDetails(String cardNumber, String cardExpiry, String cardHolderName) {
        List<String> errors = new java.util.ArrayList<>();

        if (cardNumber == null || cardNumber.isBlank()) {
            errors.add("cardNumber is required for CARD payment methods");
        } else if (!PaymentValidationService.isValidLuhn(cardNumber)) {
            errors.add("cardNumber is invalid");
        }

        errors.addAll(validateCardExpiryAndHolder(cardExpiry, cardHolderName));

        if (!errors.isEmpty()) {
            throw new com.payment.server.exception.PaymentValidationException(errors);
        }
    }

    private List<String> validateCardExpiryAndHolder(String cardExpiry, String cardHolderName) {
        List<String> errors = new java.util.ArrayList<>();

        if (cardHolderName == null || cardHolderName.isBlank()) {
            errors.add("cardHolderName is required for CARD payment methods");
        }

        if (cardExpiry == null || !cardExpiry.matches("(0[1-9]|1[0-2])/[0-9]{4}")) {
            errors.add("cardExpiry must be in MM/YYYY format");
        } else {
            String[] parts = cardExpiry.split("/");
            java.time.YearMonth expiryMonth = java.time.YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            if (expiryMonth.isBefore(java.time.YearMonth.now())) {
                errors.add("Card has already expired");
            }
        }

        return errors;
    }

    /**
     * Finds a user's payment method by id, ensuring it belongs to them.
     * Throws {@link PaymentMethodNotFoundException} if it doesn't exist or
     * belongs to a different user (avoids leaking existence of other users'
     * methods).
     */
    private PaymentMethod getOwnedPaymentMethod(int userId, int methodId) {
        PaymentMethod method = paymentMethodRepository.findById(methodId);
        if (method == null || method.getUserId() != userId) {
            throw new PaymentMethodNotFoundException(methodId);
        }
        return method;
    }

    /**
     * Edits the type-specific detail (upiId / cardNumber / linkedBankName)
     * and/or default flag of an existing payment method. The method's type
     * and underlying bank account are immutable once created.
     */
    public PaymentMethod updatePaymentMethod(int userId, int methodId, UpdatePaymentMethodRequest request) {
        getUserById(userId);
        PaymentMethod method = getOwnedPaymentMethod(userId, methodId);

        if (PaymentMethod.TYPE_UPI.equals(method.getType())) {
            method.setUpiId(request.getUpiId());
        } else if (PaymentMethod.TYPE_CARD.equals(method.getType())) {
            // Expiry/holder name can be edited on their own, but if the card
            // number itself is being replaced it must pass the same checks
            // as when the method was first added.
            String cardExpiry = request.getCardExpiry() != null ? request.getCardExpiry() : method.getCardExpiry();
            String cardHolderName = request.getCardHolderName() != null ? request.getCardHolderName()
                    : method.getCardHolderName();

            if (request.getCardNumber() != null && !request.getCardNumber().isBlank()) {
                validateCardDetails(request.getCardNumber(), cardExpiry, cardHolderName);
                String digitsOnly = request.getCardNumber().replaceAll("\\D", "");
                method.setCardLast4(digitsOnly.substring(digitsOnly.length() - 4));
                // Opaque token only - raw card number is never persisted.
                method.setCardToken(OtpHashUtil.hash(request.getCardNumber()));
            } else if (request.getCardExpiry() != null || request.getCardHolderName() != null) {
                List<String> errors = validateCardExpiryAndHolder(cardExpiry, cardHolderName);
                if (!errors.isEmpty()) {
                    throw new com.payment.server.exception.PaymentValidationException(errors);
                }
            }

            method.setCardExpiry(cardExpiry);
            method.setCardHolderName(cardHolderName);
        } else if (PaymentMethod.TYPE_NETBANKING.equals(method.getType())) {
            method.setLinkedBankName(request.getLinkedBankName());
        }

        method.setDefault(request.isDefault());
        if (request.isDefault()) {
            paymentMethodRepository.clearDefaultForUser(userId);
        }

        paymentMethodRepository.update(method);
        return method;
    }

    /**
     * Removes a payment method from a user's profile. Past payments keep
     * their historical sourcePaymentMethodId reference (no FK constraint),
     * so deleting a method never rewrites payment history.
     */
    public void deletePaymentMethod(int userId, int methodId) {
        getUserById(userId);
        getOwnedPaymentMethod(userId, methodId);
        paymentMethodRepository.deleteById(methodId);
    }
}
