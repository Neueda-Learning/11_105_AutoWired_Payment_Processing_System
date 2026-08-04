package com.payment.server.exception;

public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String email) {
        super("A user already exists with email: " + email);
    }
}
