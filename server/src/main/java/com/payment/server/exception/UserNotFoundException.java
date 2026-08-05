package com.payment.server.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(int id) {
        super("User not found with id: " + id);
    }
}
