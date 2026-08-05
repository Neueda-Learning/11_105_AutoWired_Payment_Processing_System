package com.payment.server.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/payments/initiate} - starts a payment
 * and triggers a PIN/OTP auth challenge. See payment-system-v2-design.md
 * section 5.
 */
public class InitiatePaymentRequest {

    @NotNull(message = "payerUserId is required")
    private Integer payerUserId;

    private Integer payeeUserId;

    @NotBlank(message = "sourceAccount is required")
    private String sourceAccount;

    @NotBlank(message = "destinationAccount is required")
    private String destinationAccount;

    private Integer sourcePaymentMethodId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a valid 3-letter ISO code")
    private String currency;

    @NotBlank(message = "paymentMethod is required")
    @Pattern(regexp = "UPI|NETBANKING|CREDIT_CARD", message = "paymentMethod must be UPI, NETBANKING, or CREDIT_CARD")
    private String paymentMethod;

    private String reference;
    private String idempotencyKey;

    @NotBlank(message = "authMethod is required")
    @Pattern(regexp = "PIN|OTP", message = "authMethod must be PIN or OTP")
    private String authMethod;

    // Method-specific fields, mirrors CreatePaymentRequest.
    private String cardNumber;
    private String cardExpiry;
    private String cardHolderName;

    // Only required when paymentMethod == CREDIT_CARD. Verified once at
    // initiate-time (see AuthenticationService) and never persisted anywhere
    // - it must never be stored on the Payment entity or in the database.
    @Pattern(regexp = "\\d{3,4}", message = "cvv must be 3-4 digits")
    private String cvv;

    private String upiId;
    private String bankName;

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

    public Integer getSourcePaymentMethodId() {
        return sourcePaymentMethodId;
    }

    public void setSourcePaymentMethodId(Integer sourcePaymentMethodId) {
        this.sourcePaymentMethodId = sourcePaymentMethodId;
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

    public String getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public void setCardExpiry(String cardExpiry) {
        this.cardExpiry = cardExpiry;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
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
}
