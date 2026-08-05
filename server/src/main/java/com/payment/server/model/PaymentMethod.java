package com.payment.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A way to initiate a payment from a bank account: UPI handle, saved card,
 * or net-banking link. Not itself a store of money - it's a route to the
 * underlying BankAccount. See new-docs/payment-system-v2-design.md, section 3.
 */
public class PaymentMethod {

    public static final String TYPE_UPI = "UPI";
    public static final String TYPE_CARD = "CARD";
    public static final String TYPE_NETBANKING = "NETBANKING";

    private int id;
    private int userId;
    private int bankAccountId;
    private String type; // UPI / CARD / NETBANKING

    private String upiId;

    // Masked/safe card details - raw card number/token never stored here.
    private String cardLast4;
    private String cardToken;

    private String linkedBankName;
    private boolean isDefault;

    public PaymentMethod() {
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

    public int getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(int bankAccountId) {
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

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    @JsonIgnore
    public String getCardToken() {
        return cardToken;
    }

    public void setCardToken(String cardToken) {
        this.cardToken = cardToken;
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
