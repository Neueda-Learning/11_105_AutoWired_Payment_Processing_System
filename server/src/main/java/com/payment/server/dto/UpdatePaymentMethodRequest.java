package com.payment.server.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code PUT /api/users/{id}/payment-methods/{methodId}} -
 * edit an existing UPI/card/net-banking method. The {@code type} and
 * {@code bankAccountId} of a method are immutable after creation; only the
 * type-specific detail field(s) and the default flag can be changed.
 */
public class UpdatePaymentMethodRequest {

    // Only used when the method's type == UPI.
    private String upiId;

    // Only used when the method's type == CARD. Raw card number/token are
    // never persisted here - only masked cardLast4 (+ opaque cardToken) are
    // recomputed and stored.
    private String cardNumber;

    // Only used when the method's type == CARD. Blank is allowed here (and
    // rejected for CARD methods in UserService) so that UPI/NETBANKING edits
    // - which send an empty string for this field - don't fail bean
    // validation before type-specific checks even run.
    @Pattern(regexp = "^$|(0[1-9]|1[0-2])/[0-9]{4}", message = "cardExpiry must be in MM/YYYY format")
    private String cardExpiry;

    private String cardHolderName;

    // Only used when the method's type == NETBANKING.
    private String linkedBankName;

    private boolean isDefault;

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
