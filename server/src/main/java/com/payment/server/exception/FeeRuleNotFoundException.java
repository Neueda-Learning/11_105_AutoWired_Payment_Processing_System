package com.payment.server.exception;

public class FeeRuleNotFoundException extends RuntimeException {

    public FeeRuleNotFoundException(int id) {
        super("Fee rule not found with id: " + id);
    }
}
