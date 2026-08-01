package com.payment.server.exception;

import java.util.List;

public class PaymentValidationException extends RuntimeException {

    private final List<String> errors;

    public PaymentValidationException(List<String> errors) {
        super("Payment validation failed: " + String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
