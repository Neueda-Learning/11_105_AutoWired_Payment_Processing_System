package com.payment.server.exception;

public class DuplicatePaymentException extends RuntimeException {

    private final int existingPaymentId;

    public DuplicatePaymentException(int existingPaymentId) {
        super("Payment already exists for the provided idempotency key");
        this.existingPaymentId = existingPaymentId;
    }

    public int getExistingPaymentId() {
        return existingPaymentId;
    }
}
