package com.payment.server.exception;

public class PaymentMethodNotFoundException extends RuntimeException {

    public PaymentMethodNotFoundException(int id) {
        super("Payment method not found with id: " + id);
    }
}
