package com.payment.server.exception;

/**
 * Thrown when no pending auth challenge exists for a payment, or the
 * challenge is expired/already resolved (see AuthChallenge.status).
 */
public class AuthChallengeExpiredException extends RuntimeException {

    public AuthChallengeExpiredException(int paymentId) {
        super("Authentication challenge for payment " + paymentId + " is expired or no longer valid");
    }
}
