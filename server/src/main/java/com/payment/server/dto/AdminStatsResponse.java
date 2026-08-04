package com.payment.server.dto;

import java.math.BigDecimal;

/**
 * Platform-wide figures shown on the admin dashboard: total payment volume
 * and count across all users, and the admin's earnings from the processing
 * fee (0.2% of each payment, counted only for COMPLETED payments).
 */
public class AdminStatsResponse {

    private long totalPaymentCount;
    private BigDecimal totalVolume;
    private BigDecimal totalFeeEarnings;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(long totalPaymentCount, BigDecimal totalVolume, BigDecimal totalFeeEarnings) {
        this.totalPaymentCount = totalPaymentCount;
        this.totalVolume = totalVolume;
        this.totalFeeEarnings = totalFeeEarnings;
    }

    public long getTotalPaymentCount() {
        return totalPaymentCount;
    }

    public void setTotalPaymentCount(long totalPaymentCount) {
        this.totalPaymentCount = totalPaymentCount;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public BigDecimal getTotalFeeEarnings() {
        return totalFeeEarnings;
    }

    public void setTotalFeeEarnings(BigDecimal totalFeeEarnings) {
        this.totalFeeEarnings = totalFeeEarnings;
    }
}
