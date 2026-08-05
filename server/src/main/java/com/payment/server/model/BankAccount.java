package com.payment.server.model;

import java.math.BigDecimal;

/**
 * A bank account belonging to a User; has a simulated balance, account
 * number and IFSC/routing info. See
 * new-docs/payment-system-v2-design.md, section 3.
 */
public class BankAccount {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_CLOSED = "CLOSED";

    private int id;
    private int userId;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private BigDecimal balance;
    private boolean isPrimary;
    private String status; // ACTIVE / FROZEN / CLOSED

    public BankAccount() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
