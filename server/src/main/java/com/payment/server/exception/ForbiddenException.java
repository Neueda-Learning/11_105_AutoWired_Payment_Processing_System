package com.payment.server.exception;

/**
 * Thrown when a caller who is authenticated (a known user) tries to access an
 * endpoint that requires a specific role (e.g. ADMIN) they don't have.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
