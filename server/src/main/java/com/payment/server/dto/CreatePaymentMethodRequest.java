package com.payment.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code POST /api/users/{id}/payment-methods} - add a
 * UPI/card/net-banking method drawing from one of the user's bank accounts.
 */
public class CreatePaymentMethodRequest {

    @NotNull(message = "bankAccountId is required")
    private Integer bankAccountId;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "UPI|CARD|NETBANKING", message = "type must be UPI, CARD, or NETBANKING")
    private String type;

    // Only required when type == UPI.
    private String upiId;

    // Only required when type == CARD. Raw card number/token are never
    // persisted here - only masked cardLast4 (+ opaque cardToken) are stored.
    private String cardNumber;

    // Only required when type == CARD. Expiry and holder name are not
    // sensitive on their own (unlike the raw number/CVV) so they're safe to
    // persist and reuse at payment time. Blank is allowed here (and rejected
    // in UserService.validateCardExpiryAndHolder for CARD requests) so that
    // UPI/NETBANKING submissions - which always send an empty string for
    // this field - don't fail bean validation before type-specific checks
    // even run.
    @Pattern(regexp = "^$|(0[1-9]|1[0-2])/[0-9]{4}", message = "cardExpiry must be in MM/YYYY format")
    private String cardExpiry;

    private String cardHolderName;

    // Only required when type == NETBANKING.
    private String linkedBankName;

    private boolean isDefault;

    public Integer getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Integer bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
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

    public String getLinkedBankName() {
        return linkedBankName;
    }

    public void setLinkedBankName(String linkedBankName) {
        this.linkedBankName = linkedBankName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
