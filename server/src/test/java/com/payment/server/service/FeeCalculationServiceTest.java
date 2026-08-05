package com.payment.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payment.server.model.TransactionFeeRule;
import com.payment.server.repository.TransactionFeeRuleRepository;
import com.payment.server.service.FeeCalculationService.FeeResult;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceTest {

    @Mock
    private TransactionFeeRuleRepository feeRuleRepository;

    @Test
    void calculateFeeAppliesPercentageRuleWhenMatched() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod("UPI");
        rule.setFeeType("PERCENTAGE");
        rule.setFeeValue(new BigDecimal("2.0")); // 2%
        rule.setMinAmount(BigDecimal.ZERO);
        rule.setMaxAmount(new BigDecimal("100000"));

        when(feeRuleRepository.findActive()).thenReturn(List.of(rule));

        FeeResult result = service.calculateFee("UPI", new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("20.00"), result.getFeeAmount());
        assertEquals(new BigDecimal("2.0"), result.getFeePercentage());
        assertEquals(new BigDecimal("980.00"), result.getNetAmount());
    }

    @Test
    void calculateFeeAppliesFlatRuleWhenMatched() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod("CREDIT_CARD");
        rule.setFeeType("FLAT");
        rule.setFeeValue(new BigDecimal("25.00"));
        rule.setMinAmount(BigDecimal.ZERO);

        when(feeRuleRepository.findActive()).thenReturn(List.of(rule));

        FeeResult result = service.calculateFee("CREDIT_CARD", new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("25.00"), result.getFeeAmount());
        assertEquals(new BigDecimal("975.00"), result.getNetAmount());
    }

    @Test
    void calculateFeeAppliesMinCapWhenFeeIsTooLow() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod("UPI");
        rule.setFeeType("PERCENTAGE");
        rule.setFeeValue(new BigDecimal("0.5")); // 0.5%
        rule.setMinFeeCap(new BigDecimal("10.00"));

        when(feeRuleRepository.findActive()).thenReturn(List.of(rule));

        // 0.5% of 100 = 0.50, but min cap is 10.00
        FeeResult result = service.calculateFee("UPI", new BigDecimal("100.00"));

        assertEquals(new BigDecimal("10.00"), result.getFeeAmount());
        assertEquals(new BigDecimal("90.00"), result.getNetAmount());
    }

    @Test
    void calculateFeeAppliesMaxCapWhenFeeIsTooHigh() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod("UPI");
        rule.setFeeType("PERCENTAGE");
        rule.setFeeValue(new BigDecimal("3.0")); // 3%
        rule.setMaxFeeCap(new BigDecimal("50.00"));

        when(feeRuleRepository.findActive()).thenReturn(List.of(rule));

        // 3% of 5000 = 150.00, but max cap is 50.00
        FeeResult result = service.calculateFee("UPI", new BigDecimal("5000.00"));

        assertEquals(new BigDecimal("50.00"), result.getFeeAmount());
        assertEquals(new BigDecimal("4950.00"), result.getNetAmount());
    }

    @Test
    void calculateFeeUsesDefaultWhenNoRuleMatches() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        when(feeRuleRepository.findActive()).thenReturn(Collections.emptyList());

        // Default is 1% capped at 500
        FeeResult result = service.calculateFee("NETBANKING", new BigDecimal("1000.00"));

        assertEquals(new BigDecimal("10.00"), result.getFeeAmount());
        assertEquals(new BigDecimal("1.0"), result.getFeePercentage());
        assertEquals(new BigDecimal("990.00"), result.getNetAmount());
    }

    @Test
    void calculateFeeAppliesDefaultMaxCapForLargeAmounts() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        when(feeRuleRepository.findActive()).thenReturn(Collections.emptyList());

        // 1% of 100000 = 1000, but default max cap is 500
        FeeResult result = service.calculateFee("UPI", new BigDecimal("100000.00"));

        assertEquals(new BigDecimal("500.00"), result.getFeeAmount());
        assertEquals(new BigDecimal("99500.00"), result.getNetAmount());
    }

    @Test
    void calculateFeePrefersSpecificMethodOverAllRule() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        TransactionFeeRule allRule = new TransactionFeeRule();
        allRule.setPaymentMethod("ALL");
        allRule.setFeeType("PERCENTAGE");
        allRule.setFeeValue(new BigDecimal("2.0"));

        TransactionFeeRule specificRule = new TransactionFeeRule();
        specificRule.setPaymentMethod("UPI");
        specificRule.setFeeType("PERCENTAGE");
        specificRule.setFeeValue(new BigDecimal("1.5"));

        when(feeRuleRepository.findActive()).thenReturn(List.of(allRule, specificRule));

        FeeResult result = service.calculateFee("UPI", new BigDecimal("1000.00"));

        // Should use the specific UPI rule (1.5%)
        assertEquals(new BigDecimal("15.00"), result.getFeeAmount());
    }

    @Test
    void calculateFeeMatchesAmountRange() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod("UPI");
        rule.setFeeType("PERCENTAGE");
        rule.setFeeValue(new BigDecimal("1.0"));
        rule.setMinAmount(new BigDecimal("500.00"));
        rule.setMaxAmount(new BigDecimal("5000.00"));

        when(feeRuleRepository.findActive()).thenReturn(List.of(rule));

        // Amount within range
        FeeResult result1 = service.calculateFee("UPI", new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("10.00"), result1.getFeeAmount());

        // Amount below range - should use default
        FeeResult result2 = service.calculateFee("UPI", new BigDecimal("100.00"));
        assertEquals(new BigDecimal("1.00"), result2.getFeeAmount()); // 1% default

        // Amount above range - should use default
        FeeResult result3 = service.calculateFee("UPI", new BigDecimal("10000.00"));
        assertEquals(new BigDecimal("100.00"), result3.getFeeAmount()); // 1% default
    }

    @Test
    void calculateFeeRespectsEffectiveDateRange() {
        FeeCalculationService service = new FeeCalculationService(feeRuleRepository);

        TransactionFeeRule futureRule = new TransactionFeeRule();
        futureRule.setPaymentMethod("UPI");
        futureRule.setFeeType("PERCENTAGE");
        futureRule.setFeeValue(new BigDecimal("0.5"));
        futureRule.setEffectiveFrom(LocalDateTime.now().plusDays(1)); // Future rule

        when(feeRuleRepository.findActive()).thenReturn(List.of(futureRule));

        // Should fall back to default since rule is not yet effective
        FeeResult result = service.calculateFee("UPI", new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("10.00"), result.getFeeAmount()); // 1% default
    }
}
