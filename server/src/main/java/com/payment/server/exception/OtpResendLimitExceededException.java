package com.payment.server.exception;

/**
 * Thrown when a payment has already exceeded the allowed number of OTP
 * resend requests (rate-limiting per payment-system-v2-design.md section 10).
 */
public class OtpResendLimitExceededException extends RuntimeException {

    public OtpResendLimitExceededException(int paymentId) {
        super("OTP resend limit exceeded for payment " + paymentId);
    }
}
