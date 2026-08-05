package com.payment.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin-managed rule describing how much fee the bank charges for a
 * transaction, based on payment method and amount slab. Looked up by
 * {@code FeeCalculationService} instead of hardcoding percentages inline.
 */
public class TransactionFeeRule {

    private int id;
    private String paymentMethod; // UPI / NETBANKING / CREDIT_CARD / ALL
    private BigDecimal minAmount;
    private BigDecimal maxAmount; // nullable = no upper bound
    private String feeType; // FLAT / PERCENTAGE
    private BigDecimal feeValue; // flat amount, or percentage (e.g. 1.5 = 1.5%)
    private BigDecimal minFeeCap; // nullable
    private BigDecimal maxFeeCap; // nullable
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo; // nullable = open-ended
    private boolean active = true;

    public TransactionFeeRule() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getFeeValue() {
        return feeValue;
    }

    public void setFeeValue(BigDecimal feeValue) {
        this.feeValue = feeValue;
    }

    public BigDecimal getMinFeeCap() {
        return minFeeCap;
    }

    public void setMinFeeCap(BigDecimal minFeeCap) {
        this.minFeeCap = minFeeCap;
    }

    public BigDecimal getMaxFeeCap() {
        return maxFeeCap;
    }

    public void setMaxFeeCap(BigDecimal maxFeeCap) {
        this.maxFeeCap = maxFeeCap;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
