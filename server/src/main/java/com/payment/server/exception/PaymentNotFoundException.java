package com.payment.server.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(int id) {
        super("Payment not found with id: " + id);
    }
}
