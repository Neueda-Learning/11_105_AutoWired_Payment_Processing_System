package com.payment.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Payment {

    private int id;
    private String sourceAccount;
    private String destinationAccount;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod; // UPI / NETBANKING / CREDIT_CARD
    private String status; // CREATED, VALIDATED, SENT, COMPLETED, FAILED
    private int riskScore;
    private String reference;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Raw card number - used only in-memory for validation, never persisted
    // and never serialized in API responses. Cleared once validation runs.
    private String cardNumber;

    // Card holder name is not sensitive on its own (unlike the raw number) -
    // it IS persisted so it survives the DB round-trip between initiating a
    // payment and completing PIN/OTP authentication (see AuthenticationService
    // / PaymentService.completeAuthenticatedPayment). Still not serialized in
    // API responses (@JsonIgnore below), just not stripped before persisting.
    private String cardHolderName;

    // Masked/non-sensitive card details - safe to persist and return.
    private String cardLast4;
    private String cardExpiry;

    // UPI virtual payment address, e.g. "name@bank" - safe to persist and return.
    private String upiId;

    // Net banking bank name - safe to persist and return.
    private String bankName;

    // Dynamic transaction fee (see TransactionFeeRule / FeeCalculationService).
    private BigDecimal feeAmount;
    private BigDecimal feePercentage;
    private BigDecimal netAmount;

    // Who's paying whom (nullable - legacy payments may only have account
    // numbers without linked User rows). See User / payment-system-v2-design.md.
    private Integer payerUserId;
    private Integer payeeUserId;

    // Which UPI/card/netbanking route (PaymentMethod) was used to initiate this
    // payment.
    private Integer sourcePaymentMethodId;

    // What the payer sends before the fee is deducted; amount stays as the
    // legacy field but grossAmount mirrors it explicitly for the new fee model.
    private BigDecimal grossAmount;

    // PIN/OTP authentication gate status - PENDING / VERIFIED / FAILED.
    // See AuthChallenge / payment-system-v2-design.md section 5.
    private String authenticationStatus;

    public Payment() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(String sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(String destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @JsonIgnore
    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @JsonIgnore
    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public void setCardExpiry(String cardExpiry) {
        this.cardExpiry = cardExpiry;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    public void setFeePercentage(BigDecimal feePercentage) {
        this.feePercentage = feePercentage;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getPayerUserId() {
        return payerUserId;
    }

    public void setPayerUserId(Integer payerUserId) {
        this.payerUserId = payerUserId;
    }

    public Integer getPayeeUserId() {
        return payeeUserId;
    }

    public void setPayeeUserId(Integer payeeUserId) {
        this.payeeUserId = payeeUserId;
    }

    public Integer getSourcePaymentMethodId() {
        return sourcePaymentMethodId;
    }

    public void setSourcePaymentMethodId(Integer sourcePaymentMethodId) {
        this.sourcePaymentMethodId = sourcePaymentMethodId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public String getAuthenticationStatus() {
        return authenticationStatus;
    }

    public void setAuthenticationStatus(String authenticationStatus) {
        this.authenticationStatus = authenticationStatus;
    }
}
