package com.payment.server.exception;

public class BankAccountNotFoundException extends RuntimeException {

    public BankAccountNotFoundException(int id) {
        super("Bank account not found with id: " + id);
    }
}
