package com.payment.server.exception;

/**
 * Thrown when a user attempts to change their PIN but supplies the wrong
 * current PIN.
 */
public class IncorrectPinException extends RuntimeException {

    public IncorrectPinException() {
        super("Current PIN is incorrect");
    }
}
