package com.payment.server.exception;

/**
 * Thrown when a PIN/OTP authentication attempt fails during the
 * CREATED-phase auth gate. See payment-system-v2-design.md section 5.
 */
public class AuthenticationFailedException extends RuntimeException {

    private final int paymentId;
    private final int attemptsRemaining;
    private final boolean locked;

    public AuthenticationFailedException(int paymentId, int attemptsRemaining, boolean locked) {
        super(locked
                ? "Authentication failed - maximum attempts exceeded, payment marked as FAILED"
                : "Authentication failed - incorrect PIN/OTP, " + attemptsRemaining + " attempt(s) remaining");
        this.paymentId = paymentId;
        this.attemptsRemaining = attemptsRemaining;
        this.locked = locked;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }

    public boolean isLocked() {
        return locked;
    }
}
