package com.payment.server.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.payment.server.model.ErrorCode;

@Repository
public class ErrorCodeRepository {

    private final Map<String, ErrorCode> errorCodes = new LinkedHashMap<>();

    public ErrorCodeRepository() {
        add(new ErrorCode("VALIDATION_FAILED", "Payment failed validation checks", 400, "ERROR"));
        add(new ErrorCode("INSUFFICIENT_FUNDS", "Source account has insufficient funds", 400, "ERROR"));
        add(new ErrorCode("INVALID_ACCOUNT", "Account number is invalid or doesn't exist", 400, "ERROR"));
        add(new ErrorCode("INVALID_CURRENCY", "Currency code is not supported", 400, "ERROR"));
        add(new ErrorCode("INVALID_AMOUNT", "Amount is zero, negative, or invalid", 400, "ERROR"));
        add(new ErrorCode("DUPLICATE_PAYMENT", "Payment with same idempotency key exists", 409, "WARN"));
        add(new ErrorCode("INVALID_STATUS_TRANSITION", "Cannot transition from current status to requested status",
                400, "ERROR"));
        add(new ErrorCode("PAYMENT_NOT_FOUND", "Payment ID does not exist", 404, "ERROR"));
        add(new ErrorCode("PROCESSING_ERROR", "Internal error during payment processing", 500, "ERROR"));
        add(new ErrorCode("NETWORK_ERROR", "Communication failure with payment network", 503, "ERROR"));
    }

    private void add(ErrorCode errorCode) {
        errorCodes.put(errorCode.getCode(), errorCode);
    }

    public List<ErrorCode> findAll() {
        return List.copyOf(errorCodes.values());
    }

    public ErrorCode findByCode(String code) {
        return errorCodes.get(code);
    }
}
