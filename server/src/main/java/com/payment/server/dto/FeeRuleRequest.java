package com.payment.server.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating/updating a {@code TransactionFeeRule} via the
 * admin API ({@code POST /api/fee-rules}).
 */
public class FeeRuleRequest {

    @NotBlank
    private String paymentMethod;

    @NotNull
    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    @NotBlank
    private String feeType;

    @NotNull
    private BigDecimal feeValue;

    private BigDecimal minFeeCap;
    private BigDecimal maxFeeCap;
    private boolean active = true;

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
