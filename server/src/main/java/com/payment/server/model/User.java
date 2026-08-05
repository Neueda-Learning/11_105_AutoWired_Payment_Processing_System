package com.payment.server.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A person with an account at the bank. Can be a payer or a payee.
 * See new-docs/payment-system-v2-design.md, section 3.
 */
public class User {

    public static final String KYC_PENDING = "PENDING";
    public static final String KYC_VERIFIED = "VERIFIED";

    private int id;
    private String fullName;
    private String email;
    private String phone;

    // Hashed 4-6 digit PIN used for payment authentication - never store plain.
    private String pinHash;

    private String kycStatus; // PENDING / VERIFIED
    private LocalDateTime createdAt;

    // Fraud/limits extensions - see payment-system-v2-design.md section 9.
    private java.math.BigDecimal dailyLimit;
    private String country;

    public User() {
    }

    public User(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.kycStatus = KYC_PENDING;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @JsonIgnore
    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.math.BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(java.math.BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
