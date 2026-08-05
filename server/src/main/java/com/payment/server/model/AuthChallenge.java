package com.payment.server.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A PIN/OTP authentication challenge issued for a payment before it can
 * proceed to validation - the payer must "prove it's really them" before
 * money moves. See new-docs/payment-system-v2-design.md, section 5.
 */
public class AuthChallenge {

    public static final String METHOD_PIN = "PIN";
    public static final String METHOD_OTP = "OTP";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private int id;
    private int paymentId;
    private String method; // PIN / OTP

    // Hashed OTP code (never store plain); n/a for PIN challenges.
    private String codeHash;

    private LocalDateTime expiresAt; // OTP only
    private int attempts;
    private int maxAttempts = 3;
    private String status; // PENDING / VERIFIED / FAILED / EXPIRED

    public AuthChallenge() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @JsonIgnore
    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
