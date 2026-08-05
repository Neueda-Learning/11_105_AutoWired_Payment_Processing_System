package com.payment.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payment.server.dto.CreateBankAccountRequest;
import com.payment.server.dto.CreatePaymentMethodRequest;
import com.payment.server.dto.CreateUserRequest;
import com.payment.server.dto.UpdatePinRequest;
import com.payment.server.exception.BankAccountNotFoundException;
import com.payment.server.exception.DuplicateUserException;
import com.payment.server.exception.IncorrectPinException;
import com.payment.server.exception.UserNotFoundException;
import com.payment.server.model.BankAccount;
import com.payment.server.model.PaymentMethod;
import com.payment.server.model.User;
import com.payment.server.repository.BankAccountRepository;
import com.payment.server.repository.PaymentMethodRepository;
import com.payment.server.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Test
    void registerUserCreatesNewUserWithHashedPin() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        CreateUserRequest request = new CreateUserRequest();
        request.setFullName("Alice");
        request.setEmail("alice@example.com");
        request.setPhone("555-0100");
        request.setPin("1234");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(null);

        User result = service.registerUser(request);

        assertNotNull(result);
        assertEquals("Alice", result.getFullName());
        assertEquals("alice@example.com", result.getEmail());
        assertNotNull(result.getPinHash());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUserThrowsWhenEmailAlreadyExists() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("existing@example.com");

        User existingUser = new User("Existing", "existing@example.com", "555-0100");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(existingUser);

        assertThrows(DuplicateUserException.class, () -> service.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserByIdReturnsUser() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        when(userRepository.findById(1)).thenReturn(user);

        User result = service.getUserById(1);

        assertEquals(1, result.getId());
        assertEquals("Alice", result.getFullName());
    }

    @Test
    void getUserByIdThrowsWhenNotFound() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        when(userRepository.findById(999)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> service.getUserById(999));
    }

    @Test
    void getAllUsersReturnsAllUsers() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user1 = new User("Alice", "alice@example.com", "555-0100");
        User user2 = new User("Bob", "bob@example.com", "555-0200");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<User> result = service.getAllUsers();

        assertEquals(2, result.size());
    }

    @Test
    void updatePinSucceedsWithCorrectCurrentPin() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        String currentPin = "1234";
        String newPin = "5678";

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);
        user.setPinHash(OtpHashUtil.hash(currentPin));

        UpdatePinRequest request = new UpdatePinRequest();
        request.setCurrentPin(currentPin);
        request.setNewPin(newPin);

        when(userRepository.findById(1)).thenReturn(user);

        User result = service.updatePin(1, request);

        assertNotNull(result.getPinHash());
        assertNotEquals(OtpHashUtil.hash(currentPin), result.getPinHash());
        verify(userRepository).updatePinHash(eq(1), anyString());
    }

    @Test
    void updatePinThrowsWhenCurrentPinIncorrect() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);
        user.setPinHash(OtpHashUtil.hash("1234"));

        UpdatePinRequest request = new UpdatePinRequest();
        request.setCurrentPin("wrong-pin");
        request.setNewPin("5678");

        when(userRepository.findById(1)).thenReturn(user);

        assertThrows(IncorrectPinException.class, () -> service.updatePin(1, request));
        verify(userRepository, never()).updatePinHash(anyInt(), anyString());
    }

    @Test
    void getBankAccountsReturnsUserAccounts() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        BankAccount account1 = new BankAccount();
        account1.setUserId(1);
        BankAccount account2 = new BankAccount();
        account2.setUserId(1);

        when(userRepository.findById(1)).thenReturn(user);
        when(bankAccountRepository.findByUserId(1)).thenReturn(Arrays.asList(account1, account2));

        List<BankAccount> result = service.getBankAccounts(1);

        assertEquals(2, result.size());
    }

    @Test
    void addBankAccountCreatesNewAccount() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setAccountNumber("ACC-100");
        request.setIfscCode("IFSC001");
        request.setBankName("Test Bank");
        request.setBalance(new BigDecimal("10000.00"));
        request.setPrimary(true);

        when(userRepository.findById(1)).thenReturn(user);

        BankAccount result = service.addBankAccount(1, request);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("ACC-100", result.getAccountNumber());
        assertTrue(result.isPrimary());
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    void addBankAccountThrowsWhenUserNotFound() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        CreateBankAccountRequest request = new CreateBankAccountRequest();
        request.setAccountNumber("ACC-100");

        when(userRepository.findById(999)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> service.addBankAccount(999, request));
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void addPaymentMethodCreatesNewMethod() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        BankAccount account = new BankAccount();
        account.setId(10);
        account.setUserId(1);

        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest();
        request.setBankAccountId(10);
        request.setType("UPI");
        request.setUpiId("alice@upi");
        request.setDefault(false);

        when(userRepository.findById(1)).thenReturn(user);
        when(bankAccountRepository.findById(10)).thenReturn(account);

        PaymentMethod result = service.addPaymentMethod(1, request);

        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals("UPI", result.getType());
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    void addPaymentMethodClearsDefaultWhenNewMethodIsDefault() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        BankAccount account = new BankAccount();
        account.setId(10);
        account.setUserId(1);

        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest();
        request.setBankAccountId(10);
        request.setType("UPI");
        request.setDefault(true); // Setting as default

        when(userRepository.findById(1)).thenReturn(user);
        when(bankAccountRepository.findById(10)).thenReturn(account);

        service.addPaymentMethod(1, request);

        verify(paymentMethodRepository).clearDefaultForUser(1);
        verify(paymentMethodRepository).save(any(PaymentMethod.class));
    }

    @Test
    void addPaymentMethodThrowsWhenBankAccountNotFound() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest();
        request.setBankAccountId(999);

        when(userRepository.findById(1)).thenReturn(user);
        when(bankAccountRepository.findById(999)).thenReturn(null);

        assertThrows(BankAccountNotFoundException.class, () -> service.addPaymentMethod(1, request));
    }

    @Test
    void addPaymentMethodThrowsWhenBankAccountBelongsToDifferentUser() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        BankAccount account = new BankAccount();
        account.setId(10);
        account.setUserId(2); // Different user

        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest();
        request.setBankAccountId(10);

        when(userRepository.findById(1)).thenReturn(user);
        when(bankAccountRepository.findById(10)).thenReturn(account);

        assertThrows(BankAccountNotFoundException.class, () -> service.addPaymentMethod(1, request));
    }

    @Test
    void addPaymentMethodExtractsCardLast4FromCardNumber() {
        UserService service = new UserService(userRepository, bankAccountRepository, paymentMethodRepository);

        User user = new User("Alice", "alice@example.com", "555-0100");
        user.setId(1);

        BankAccount account = new BankAccount();
        account.setId(10);
        account.setUserId(1);

        CreatePaymentMethodRequest request = new CreatePaymentMethodRequest();
        request.setBankAccountId(10);
        request.setType("CREDIT_CARD");
        request.setCardNumber("4111-1111-1111-1234");
        request.setDefault(false);

        when(userRepository.findById(1)).thenReturn(user);
        when(bankAccountRepository.findById(10)).thenReturn(account);

        PaymentMethod result = service.addPaymentMethod(1, request);

        assertEquals("1234", result.getCardLast4());
        assertNotNull(result.getCardToken());
    }
}
