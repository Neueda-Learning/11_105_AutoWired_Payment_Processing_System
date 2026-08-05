package com.payment.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.payment.server.model.TransactionFeeRule;
import com.payment.server.repository.TransactionFeeRuleRepository;

/**
 * Computes the bank's transaction fee for a payment based on configurable
 * {@code TransactionFeeRule} rows instead of hardcoding a percentage inline.
 * Looks up the active rule matching (paymentMethod, amount, date) and falls
 * back to a global default rule if none matches.
 */
@Service
public class FeeCalculationService {

    private static final BigDecimal DEFAULT_FEE_PERCENTAGE = new BigDecimal("1.0"); // 1% fallback
    private static final BigDecimal DEFAULT_MAX_FEE_CAP = new BigDecimal("500.00");

    private final TransactionFeeRuleRepository feeRuleRepository;

    public FeeCalculationService(TransactionFeeRuleRepository feeRuleRepository) {
        this.feeRuleRepository = feeRuleRepository;
    }

    public static class FeeResult {
        private final BigDecimal feeAmount;
        private final BigDecimal feePercentage;
        private final BigDecimal netAmount;

        public FeeResult(BigDecimal feeAmount, BigDecimal feePercentage, BigDecimal netAmount) {
            this.feeAmount = feeAmount;
            this.feePercentage = feePercentage;
            this.netAmount = netAmount;
        }

        public BigDecimal getFeeAmount() {
            return feeAmount;
        }

        public BigDecimal getFeePercentage() {
            return feePercentage;
        }

        public BigDecimal getNetAmount() {
            return netAmount;
        }
    }

    public FeeResult calculateFee(String paymentMethod, BigDecimal amount) {
        Optional<TransactionFeeRule> matched = findMatchingRule(paymentMethod, amount);

        BigDecimal feeAmount;
        BigDecimal feePercentage;

        if (matched.isPresent()) {
            TransactionFeeRule rule = matched.get();
            if ("FLAT".equalsIgnoreCase(rule.getFeeType())) {
                feeAmount = rule.getFeeValue();
                feePercentage = amount.compareTo(BigDecimal.ZERO) > 0
                        ? feeAmount.divide(amount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;
            } else {
                feePercentage = rule.getFeeValue();
                feeAmount = amount.multiply(feePercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                feeAmount = applyCaps(feeAmount, rule.getMinFeeCap(), rule.getMaxFeeCap());
            }
        } else {
            // Fallback: default global percentage fee, capped.
            feePercentage = DEFAULT_FEE_PERCENTAGE;
            feeAmount = amount.multiply(feePercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            feeAmount = applyCaps(feeAmount, null, DEFAULT_MAX_FEE_CAP);
        }

        feeAmount = feeAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = amount.subtract(feeAmount).setScale(2, RoundingMode.HALF_UP);
        return new FeeResult(feeAmount, feePercentage, netAmount);
    }

    private BigDecimal applyCaps(BigDecimal feeAmount, BigDecimal minCap, BigDecimal maxCap) {
        BigDecimal result = feeAmount;
        if (minCap != null && result.compareTo(minCap) < 0) {
            result = minCap;
        }
        if (maxCap != null && result.compareTo(maxCap) > 0) {
            result = maxCap;
        }
        return result;
    }

    private Optional<TransactionFeeRule> findMatchingRule(String paymentMethod, BigDecimal amount) {
        LocalDateTime now = LocalDateTime.now();
        List<TransactionFeeRule> active = feeRuleRepository.findActive();

        return active.stream()
                .filter(r -> r.getPaymentMethod() != null
                        && (r.getPaymentMethod().equalsIgnoreCase(paymentMethod)
                                || r.getPaymentMethod().equalsIgnoreCase("ALL")))
                .filter(r -> r.getMinAmount() == null || amount.compareTo(r.getMinAmount()) >= 0)
                .filter(r -> r.getMaxAmount() == null || amount.compareTo(r.getMaxAmount()) <= 0)
                .filter(r -> r.getEffectiveFrom() == null || !now.isBefore(r.getEffectiveFrom()))
                .filter(r -> r.getEffectiveTo() == null || !now.isAfter(r.getEffectiveTo()))
                // Prefer a method-specific rule over an "ALL" catch-all.
                .sorted(Comparator.comparing((TransactionFeeRule r) -> "ALL".equalsIgnoreCase(r.getPaymentMethod())))
                .findFirst();
    }
}
