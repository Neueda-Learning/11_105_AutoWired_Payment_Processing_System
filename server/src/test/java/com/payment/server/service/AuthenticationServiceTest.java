package com.payment.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payment.server.dto.AuthenticatePaymentRequest;
import com.payment.server.dto.InitiatePaymentRequest;
import com.payment.server.exception.AuthChallengeExpiredException;
import com.payment.server.exception.AuthenticationFailedException;
import com.payment.server.exception.OtpResendLimitExceededException;
import com.payment.server.exception.UserNotFoundException;
import com.payment.server.model.AuthChallenge;
import com.payment.server.model.Payment;
import com.payment.server.model.User;
import com.payment.server.repository.AuthChallengeRepository;
import com.payment.server.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private AuthChallengeRepository authChallengeRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void initiatePaymentCreatesPaymentAndOtpChallengeForValidUser() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setPayerUserId(1);
        request.setPayeeUserId(2);
        request.setAmount(new BigDecimal("1000.00"));
        request.setCurrency("USD");
        request.setPaymentMethod("UPI");
        request.setAuthMethod(AuthChallenge.METHOD_OTP);

        User payer = new User("Alice", "alice@example.com", "555-0100");
        payer.setId(1);

        Payment createdPayment = new Payment();
        createdPayment.setId(100);

        when(userRepository.findById(1)).thenReturn(payer);
        when(paymentService.createPendingPayment(any(Payment.class))).thenReturn(createdPayment);

        Payment result = service.initiatePayment(request);

        assertEquals(100, result.getId());
        verify(authChallengeRepository).save(any(AuthChallenge.class));
    }

    @Test
    void initiatePaymentThrowsWhenUserNotFound() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        InitiatePaymentRequest request = new InitiatePaymentRequest();
        request.setPayerUserId(999);
        request.setAmount(new BigDecimal("1000.00"));

        when(userRepository.findById(999)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> service.initiatePayment(request));
        verify(paymentService, never()).createPendingPayment(any());
    }

    @Test
    void authenticateWithValidOtpCompletesPayment() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        Payment payment = new Payment();
        payment.setId(100);
        payment.setPayerUserId(1);

        String otp = "123456";
        AuthChallenge challenge = new AuthChallenge();
        challenge.setId(1);
        challenge.setMethod(AuthChallenge.METHOD_OTP);
        challenge.setStatus(AuthChallenge.STATUS_PENDING);
        challenge.setCodeHash(OtpHashUtil.hash(otp));
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setAttempts(0);
        challenge.setMaxAttempts(3);

        AuthenticatePaymentRequest request = new AuthenticatePaymentRequest();
        request.setOtp(otp);

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(challenge);
        when(paymentService.completeAuthenticatedPayment(100)).thenReturn(payment);

        Payment result = service.authenticate(100, request);

        assertNotNull(result);
        verify(authChallengeRepository).updateStatus(1, AuthChallenge.STATUS_VERIFIED);
        verify(paymentService).completeAuthenticatedPayment(100);
    }

    @Test
    void authenticateWithInvalidOtpIncrementsAttemptsAndThrows() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        Payment payment = new Payment();
        payment.setId(100);

        AuthChallenge challenge = new AuthChallenge();
        challenge.setId(1);
        challenge.setMethod(AuthChallenge.METHOD_OTP);
        challenge.setStatus(AuthChallenge.STATUS_PENDING);
        challenge.setCodeHash(OtpHashUtil.hash("123456"));
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setAttempts(0);
        challenge.setMaxAttempts(3);

        AuthenticatePaymentRequest request = new AuthenticatePaymentRequest();
        request.setOtp("wrong-otp");

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(challenge);

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate(100, request));

        assertEquals(2, ex.getAttemptsRemaining());
        assertFalse(ex.isLocked());
        verify(authChallengeRepository).incrementAttempts(1);
    }

    @Test
    void authenticateFailsAndLocksAfterMaxAttempts() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        Payment payment = new Payment();
        payment.setId(100);

        AuthChallenge challenge = new AuthChallenge();
        challenge.setId(1);
        challenge.setMethod(AuthChallenge.METHOD_OTP);
        challenge.setStatus(AuthChallenge.STATUS_PENDING);
        challenge.setCodeHash(OtpHashUtil.hash("123456"));
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setAttempts(2); // Already 2 attempts used
        challenge.setMaxAttempts(3);

        AuthenticatePaymentRequest request = new AuthenticatePaymentRequest();
        request.setOtp("wrong-otp");

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(challenge);

        AuthenticationFailedException ex = assertThrows(AuthenticationFailedException.class,
                () -> service.authenticate(100, request));

        assertEquals(0, ex.getAttemptsRemaining());
        assertTrue(ex.isLocked());
        verify(authChallengeRepository).updateStatus(1, AuthChallenge.STATUS_FAILED);
        verify(paymentService).failAuthentication(100, "Max authentication attempts exceeded");
    }

    @Test
    void authenticateThrowsWhenOtpExpired() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        Payment payment = new Payment();
        payment.setId(100);

        AuthChallenge challenge = new AuthChallenge();
        challenge.setId(1);
        challenge.setMethod(AuthChallenge.METHOD_OTP);
        challenge.setStatus(AuthChallenge.STATUS_PENDING);
        challenge.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired 1 minute ago

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(challenge);

        assertThrows(AuthChallengeExpiredException.class, () -> service.authenticate(100, new AuthenticatePaymentRequest()));
        verify(authChallengeRepository).updateStatus(1, AuthChallenge.STATUS_EXPIRED);
        verify(paymentService).failAuthentication(100, "OTP expired");
    }

    @Test
    void authenticateWithValidPinCompletesPayment() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        String pin = "1234";
        User payer = new User("Alice", "alice@example.com", "555-0100");
        payer.setId(1);
        payer.setPinHash(OtpHashUtil.hash(pin));

        Payment payment = new Payment();
        payment.setId(100);
        payment.setPayerUserId(1);

        AuthChallenge challenge = new AuthChallenge();
        challenge.setId(1);
        challenge.setMethod(AuthChallenge.METHOD_PIN);
        challenge.setStatus(AuthChallenge.STATUS_PENDING);

        AuthenticatePaymentRequest request = new AuthenticatePaymentRequest();
        request.setPin(pin);

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(challenge);
        when(userRepository.findById(1)).thenReturn(payer);
        when(paymentService.completeAuthenticatedPayment(100)).thenReturn(payment);

        Payment result = service.authenticate(100, request);

        assertNotNull(result);
        verify(authChallengeRepository).updateStatus(1, AuthChallenge.STATUS_VERIFIED);
    }

    @Test
    void resendOtpCreatesNewChallengeAndExpiresOld() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        User payer = new User("Alice", "alice@example.com", "555-0100");
        payer.setId(1);

        Payment payment = new Payment();
        payment.setId(100);
        payment.setPayerUserId(1);

        AuthChallenge oldChallenge = new AuthChallenge();
        oldChallenge.setId(1);
        oldChallenge.setMethod(AuthChallenge.METHOD_OTP);
        oldChallenge.setStatus(AuthChallenge.STATUS_PENDING);

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(oldChallenge);
        when(authChallengeRepository.countByPaymentId(100)).thenReturn(1L);
        when(userRepository.findById(1)).thenReturn(payer);

        Payment result = service.resendOtp(100);

        assertNotNull(result);
        verify(authChallengeRepository).updateStatus(1, AuthChallenge.STATUS_EXPIRED);
        verify(authChallengeRepository).save(any(AuthChallenge.class));
    }

    @Test
    void resendOtpThrowsWhenLimitExceeded() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        Payment payment = new Payment();
        payment.setId(100);
        payment.setPayerUserId(1);

        AuthChallenge challenge = new AuthChallenge();
        challenge.setMethod(AuthChallenge.METHOD_OTP);
        challenge.setStatus(AuthChallenge.STATUS_PENDING);

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(challenge);
        when(authChallengeRepository.countByPaymentId(100)).thenReturn(3L); // At limit

        assertThrows(OtpResendLimitExceededException.class, () -> service.resendOtp(100));
        verify(authChallengeRepository, never()).save(any(AuthChallenge.class));
    }

    @Test
    void resendOtpThrowsWhenChallengeNotPending() {
        AuthenticationService service = new AuthenticationService(paymentService, authChallengeRepository,
                userRepository);

        Payment payment = new Payment();
        payment.setId(100);

        AuthChallenge challenge = new AuthChallenge();
        challenge.setMethod(AuthChallenge.METHOD_OTP);
        challenge.setStatus(AuthChallenge.STATUS_VERIFIED); // Not pending

        when(paymentService.getPaymentById(100)).thenReturn(payment);
        when(authChallengeRepository.findLatestByPaymentId(100)).thenReturn(challenge);

        assertThrows(AuthChallengeExpiredException.class, () -> service.resendOtp(100));
    }
}
