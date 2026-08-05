package com.payment.server.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment.server.dto.AuthenticatePaymentRequest;
import com.payment.server.dto.InitiatePaymentRequest;
import com.payment.server.exception.AuthChallengeExpiredException;
import com.payment.server.exception.AuthenticationFailedException;
import com.payment.server.exception.OtpResendLimitExceededException;
import com.payment.server.exception.PaymentValidationException;
import com.payment.server.exception.UserNotFoundException;
import com.payment.server.model.AuthChallenge;
import com.payment.server.model.Payment;
import com.payment.server.model.PaymentMethod;
import com.payment.server.model.User;
import com.payment.server.repository.AuthChallengeRepository;
import com.payment.server.repository.PaymentMethodRepository;
import com.payment.server.repository.UserRepository;

/**
 * PIN/OTP authentication gate that sits inside the CREATED phase, before
 * validation runs - proves the payer is really them before money moves.
 * See payment-system-v2-design.md section 5 (Sprint B).
 */
@Service
public class AuthenticationService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    // Rate-limits abuse of the resend endpoint - see
    // payment-system-v2-design.md section 10 (OTP hygiene).
    private static final int MAX_OTP_RESENDS = 3;

    private final PaymentService paymentService;
    private final AuthChallengeRepository authChallengeRepository;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public AuthenticationService(PaymentService paymentService,
            AuthChallengeRepository authChallengeRepository,
            UserRepository userRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.paymentService = paymentService;
        this.authChallengeRepository = authChallengeRepository;
        this.userRepository = userRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Transactional
    public Payment initiatePayment(InitiatePaymentRequest request) {
        User payer = userRepository.findById(request.getPayerUserId());
        if (payer == null) {
            throw new UserNotFoundException(request.getPayerUserId());
        }

        Payment payment = new Payment();
        payment.setPayerUserId(request.getPayerUserId());
        payment.setPayeeUserId(request.getPayeeUserId());
        payment.setSourceAccount(request.getSourceAccount());
        payment.setDestinationAccount(request.getDestinationAccount());
        payment.setSourcePaymentMethodId(request.getSourcePaymentMethodId());
        payment.setAmount(request.getAmount());
        payment.setGrossAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReference(request.getReference());
        payment.setIdempotencyKey(request.getIdempotencyKey());

        if ("CREDIT_CARD".equals(request.getPaymentMethod())) {
            validateCvv(request.getCvv());

            if (request.getSourcePaymentMethodId() != null) {
                // Paying with a saved card - pull the safe (masked) details
                // that were stored when the card was added. The raw card
                // number is never available/stored here.
                PaymentMethod method = paymentMethodRepository.findById(request.getSourcePaymentMethodId());
                if (method == null || method.getUserId() != request.getPayerUserId()
                        || !PaymentMethod.TYPE_CARD.equals(method.getType())) {
                    throw new PaymentValidationException(List.of("Selected card payment method is invalid"));
                }
                payment.setCardHolderName(method.getCardHolderName());
                payment.setCardExpiry(method.getCardExpiry());
                payment.setCardLast4(method.getCardLast4());
            } else {
                // Legacy raw-entry path - full card details are supplied
                // fresh with this request and never persisted beyond this.
                payment.setCardNumber(request.getCardNumber());
                payment.setCardHolderName(request.getCardHolderName());
                payment.setCardExpiry(request.getCardExpiry());
                if (request.getCardNumber() != null) {
                    String digitsOnly = request.getCardNumber().replaceAll("\\D", "");
                    if (digitsOnly.length() >= 4) {
                        payment.setCardLast4(digitsOnly.substring(digitsOnly.length() - 4));
                    }
                }
            }
        } else if ("UPI".equals(request.getPaymentMethod())) {
            payment.setUpiId(request.getUpiId());
        } else if ("NETBANKING".equals(request.getPaymentMethod())) {
            payment.setBankName(request.getBankName());
        }

        Payment created = paymentService.createPendingPayment(payment);

        AuthChallenge challenge = new AuthChallenge();
        challenge.setPaymentId(created.getId());
        challenge.setMethod(request.getAuthMethod());
        challenge.setAttempts(0);
        challenge.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
        challenge.setStatus(AuthChallenge.STATUS_PENDING);

        if (AuthChallenge.METHOD_OTP.equals(request.getAuthMethod())) {
            String otp = generateOtp();
            challenge.setCodeHash(OtpHashUtil.hash(otp));
            challenge.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            // Email delivery is simulated (stand-in for SMS per the design doc) -
            // a real system would call an email/SMS provider here instead.
            System.out.printf("[AuthenticationService] Simulated OTP email to %s: %s (expires in %d min)%n",
                    payer.getEmail(), otp, OTP_EXPIRY_MINUTES);
        }

        authChallengeRepository.save(challenge);
        return created;
    }

    /**
     * Confirms a CVV was supplied and looks like one (3-4 digits). The value
     * itself is only ever held in this local variable/parameter - it is
     * never attached to the Payment entity and never persisted anywhere.
     */
    private void validateCvv(String cvv) {
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new PaymentValidationException(List.of("cvv must be 3-4 digits"));
        }
    }

    // noRollbackFor: on OTP expiry / max attempts exceeded, failAuthentication()
    // intentionally persists a FAILED status + audit history row before this
    // method throws. Without this, Spring's default rollback-on-RuntimeException
    // would wipe out that FAILED payment entirely (see PaymentService.createPayment
    // for the same pattern). PaymentValidationException is also included here:
    // on success, completeAuthenticatedPayment() runs validation (e.g. insufficient
    // balance) inside THIS method's transaction (Spring nests it since it's called
    // via another bean's proxy while this transaction is already active), so this
    // outer method's rollback rules - not completeAuthenticatedPayment's own -
    // decide whether the FAILED write survives.
    @Transactional(noRollbackFor = { AuthenticationFailedException.class, AuthChallengeExpiredException.class,
            PaymentValidationException.class })
    public Payment authenticate(int paymentId, AuthenticatePaymentRequest request) {
        Payment payment = paymentService.getPaymentById(paymentId);
        AuthChallenge challenge = authChallengeRepository.findLatestByPaymentId(paymentId);

        if (challenge == null || !AuthChallenge.STATUS_PENDING.equals(challenge.getStatus())) {
            throw new AuthChallengeExpiredException(paymentId);
        }

        if (AuthChallenge.METHOD_OTP.equals(challenge.getMethod())
                && challenge.getExpiresAt() != null
                && LocalDateTime.now().isAfter(challenge.getExpiresAt())) {
            authChallengeRepository.updateStatus(challenge.getId(), AuthChallenge.STATUS_EXPIRED);
            paymentService.failAuthentication(paymentId, "OTP expired");
            throw new AuthChallengeExpiredException(paymentId);
        }

        if (verifyCode(challenge, payment, request)) {
            authChallengeRepository.updateStatus(challenge.getId(), AuthChallenge.STATUS_VERIFIED);
            return paymentService.completeAuthenticatedPayment(paymentId);
        }

        authChallengeRepository.incrementAttempts(challenge.getId());
        int attemptsUsed = challenge.getAttempts() + 1;
        int attemptsRemaining = challenge.getMaxAttempts() - attemptsUsed;

        if (attemptsRemaining <= 0) {
            authChallengeRepository.updateStatus(challenge.getId(), AuthChallenge.STATUS_FAILED);
            paymentService.failAuthentication(paymentId, "Max authentication attempts exceeded");
            throw new AuthenticationFailedException(paymentId, 0, true);
        }

        throw new AuthenticationFailedException(paymentId, attemptsRemaining, false);
    }

    /**
     * Re-sends an OTP for a payment whose challenge is still pending -
     * expires the old challenge and issues a fresh code. Rate-limited to
     * MAX_OTP_RESENDS total challenges per payment to prevent abuse.
     * See payment-system-v2-design.md section 8 & 10.
     */
    @Transactional
    public Payment resendOtp(int paymentId) {
        Payment payment = paymentService.getPaymentById(paymentId);
        AuthChallenge challenge = authChallengeRepository.findLatestByPaymentId(paymentId);

        if (challenge == null || !AuthChallenge.METHOD_OTP.equals(challenge.getMethod())
                || !AuthChallenge.STATUS_PENDING.equals(challenge.getStatus())) {
            throw new AuthChallengeExpiredException(paymentId);
        }

        if (authChallengeRepository.countByPaymentId(paymentId) >= MAX_OTP_RESENDS) {
            throw new OtpResendLimitExceededException(paymentId);
        }

        if (payment.getPayerUserId() == null) {
            throw new UserNotFoundException(paymentId);
        }
        User payer = userRepository.findById(payment.getPayerUserId());
        if (payer == null) {
            throw new UserNotFoundException(payment.getPayerUserId());
        }

        // Expire the old challenge and issue a brand new one.
        authChallengeRepository.updateStatus(challenge.getId(), AuthChallenge.STATUS_EXPIRED);

        AuthChallenge newChallenge = new AuthChallenge();
        newChallenge.setPaymentId(paymentId);
        newChallenge.setMethod(AuthChallenge.METHOD_OTP);
        newChallenge.setAttempts(0);
        newChallenge.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
        newChallenge.setStatus(AuthChallenge.STATUS_PENDING);

        String otp = generateOtp();
        newChallenge.setCodeHash(OtpHashUtil.hash(otp));
        newChallenge.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        System.out.printf("[AuthenticationService] Simulated OTP resend email to %s: %s (expires in %d min)%n",
                payer.getEmail(), otp, OTP_EXPIRY_MINUTES);

        authChallengeRepository.save(newChallenge);
        return payment;
    }

    private boolean verifyCode(AuthChallenge challenge, Payment payment, AuthenticatePaymentRequest request) {
        if (AuthChallenge.METHOD_PIN.equals(challenge.getMethod())) {
            if (payment.getPayerUserId() == null || request.getPin() == null) {
                return false;
            }
            User payer = userRepository.findById(payment.getPayerUserId());
            return payer != null && payer.getPinHash() != null
                    && OtpHashUtil.matches(request.getPin(), payer.getPinHash());
        }
        if (AuthChallenge.METHOD_OTP.equals(challenge.getMethod())) {
            return request.getOtp() != null && challenge.getCodeHash() != null
                    && OtpHashUtil.matches(request.getOtp(), challenge.getCodeHash());
        }
        return false;
    }

    private String generateOtp() {
        int code = new SecureRandom().nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
