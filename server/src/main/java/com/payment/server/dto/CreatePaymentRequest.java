package com.payment.server.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreatePaymentRequest {

    @NotBlank(message = "sourceAccount is required")
    private String sourceAccount;

    @NotBlank(message = "destinationAccount is required")
    private String destinationAccount;

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

    // Only required when paymentMethod == CREDIT_CARD. Never persisted in raw
    // form - see PaymentValidationService and Payment model for masking.
    private String cardNumber;

    private String cardExpiry;

    private String cardHolderName;

    // Only required when paymentMethod == UPI.
    private String upiId;

    // Only required when paymentMethod == NETBANKING.
    private String bankName;

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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
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
