package com.payment.server.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Server-side computed summary stats, matching the client's
 * {@code PaymentStats} type (closes the gap noted in Implementation Status:
 * previously only computed client-side as a fallback).
 */
public class PaymentStats {

    private long totalCount;
    private BigDecimal totalVolume;
    private double successRate;
    private double avgRiskScore;
    private Map<String, Long> statusCounts;
    private BigDecimal totalFeesCollected;

    public PaymentStats() {
    }

    public PaymentStats(long totalCount, BigDecimal totalVolume, double successRate, double avgRiskScore,
            Map<String, Long> statusCounts, BigDecimal totalFeesCollected) {
        this.totalCount = totalCount;
        this.totalVolume = totalVolume;
        this.successRate = successRate;
        this.avgRiskScore = avgRiskScore;
        this.statusCounts = statusCounts;
        this.totalFeesCollected = totalFeesCollected;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getAvgRiskScore() {
        return avgRiskScore;
    }

    public void setAvgRiskScore(double avgRiskScore) {
        this.avgRiskScore = avgRiskScore;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public BigDecimal getTotalFeesCollected() {
        return totalFeesCollected;
    }

    public void setTotalFeesCollected(BigDecimal totalFeesCollected) {
        this.totalFeesCollected = totalFeesCollected;
    }
}
