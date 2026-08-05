package com.payment.server.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/users/{id}/bank-accounts} - link a
 * bank account to a user.
 */
public class CreateBankAccountRequest {

    @NotBlank(message = "accountNumber is required")
    private String accountNumber;

    private String ifscCode;

    @NotBlank(message = "bankName is required")
    private String bankName;

    private BigDecimal balance;

    private boolean isPrimary;

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
}
