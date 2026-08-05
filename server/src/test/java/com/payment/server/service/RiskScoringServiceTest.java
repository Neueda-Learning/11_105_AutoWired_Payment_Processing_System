package com.payment.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payment.server.model.Payment;
import com.payment.server.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class RiskScoringServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void scorePaymentReturnsZeroForLowRiskTransaction() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("1000.00")); // Below high amount threshold
        payment.setSourceAccount("ACC-100");
        payment.setPayerUserId(1);

        when(paymentRepository.countRecentByAccountExcludingPayment(anyString(), any(LocalDateTime.class), eq(100)))
                .thenReturn(0L);
        when(paymentRepository.averageAmountByPayerUserIdExcluding(eq(1), eq(100)))
                .thenReturn(new BigDecimal("1000.00"));

        int score = service.scorePayment(payment, 100);

        // No risk factors triggered
        assertEquals(0, score);
    }

    @Test
    void scorePaymentAdds40PointsForHighAmount() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("50000.00")); // At high amount threshold
        payment.setSourceAccount("ACC-100");

        when(paymentRepository.countRecentByAccountExcludingPayment(anyString(), any(LocalDateTime.class), eq(100)))
                .thenReturn(0L);

        int score = service.scorePayment(payment, 100);

        assertEquals(40, score);
    }

    @Test
    void scorePaymentAdds40PointsForHighVelocity() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setSourceAccount("ACC-100");

        // 3 or more recent transactions from same account
        when(paymentRepository.countRecentByAccountExcludingPayment(eq("ACC-100"), any(LocalDateTime.class), eq(100)))
                .thenReturn(3L);

        int score = service.scorePayment(payment, 100);

        assertEquals(40, score);
    }

    @Test
    void scorePaymentAdds30PointsForSuddenSpike() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("6000.00")); // 3x the average
        payment.setSourceAccount("ACC-100");
        payment.setPayerUserId(1);

        when(paymentRepository.countRecentByAccountExcludingPayment(anyString(), any(LocalDateTime.class), eq(100)))
                .thenReturn(0L);
        when(paymentRepository.averageAmountByPayerUserIdExcluding(eq(1), eq(100)))
                .thenReturn(new BigDecimal("2000.00")); // Average is 2000, payment is 3x

        int score = service.scorePayment(payment, 100);

        assertEquals(30, score);
    }

    @Test
    void scorePaymentCombinesMultipleRiskFactors() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("60000.00")); // High amount (40 points)
        payment.setSourceAccount("ACC-100");
        payment.setPayerUserId(1);

        // High velocity (40 points)
        when(paymentRepository.countRecentByAccountExcludingPayment(eq("ACC-100"), any(LocalDateTime.class), eq(100)))
                .thenReturn(5L);
        // Sudden spike (30 points)
        when(paymentRepository.averageAmountByPayerUserIdExcluding(eq(1), eq(100)))
                .thenReturn(new BigDecimal("15000.00")); // 60000 is 4x the average

        int score = service.scorePayment(payment, 100);

        // 40 + 40 + 30 = 110, but capped at 100
        assertEquals(100, score);
    }

    @Test
    void scorePaymentIgnoresSpikeFlagWhenNoPayerUserId() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100000.00"));
        payment.setSourceAccount("ACC-100");
        payment.setPayerUserId(null); // No user ID

        when(paymentRepository.countRecentByAccountExcludingPayment(anyString(), any(LocalDateTime.class), eq(100)))
                .thenReturn(0L);

        int score = service.scorePayment(payment, 100);

        // Only high amount (40 points), no spike check
        assertEquals(40, score);
        verify(paymentRepository, never()).averageAmountByPayerUserIdExcluding(anyInt(), anyInt());
    }

    @Test
    void scorePaymentIgnoresSpikeFlagWhenAverageIsZero() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("5000.00"));
        payment.setSourceAccount("ACC-100");
        payment.setPayerUserId(1);

        when(paymentRepository.countRecentByAccountExcludingPayment(anyString(), any(LocalDateTime.class), eq(100)))
                .thenReturn(0L);
        when(paymentRepository.averageAmountByPayerUserIdExcluding(eq(1), eq(100)))
                .thenReturn(BigDecimal.ZERO); // No historical average

        int score = service.scorePayment(payment, 100);

        // No spike points since average is zero
        assertEquals(0, score);
    }

    @Test
    void scorePaymentIgnoresSpikeFlagWhenAverageIsNull() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("5000.00"));
        payment.setSourceAccount("ACC-100");
        payment.setPayerUserId(1);

        when(paymentRepository.countRecentByAccountExcludingPayment(anyString(), any(LocalDateTime.class), eq(100)))
                .thenReturn(0L);
        when(paymentRepository.averageAmountByPayerUserIdExcluding(eq(1), eq(100)))
                .thenReturn(null); // No history

        int score = service.scorePayment(payment, 100);

        assertEquals(0, score);
    }

    @Test
    void scorePaymentDoesNotFlagWhenBelowSpikeThreshold() {
        RiskScoringService service = new RiskScoringService(paymentRepository);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("5000.00")); // Less than 3x average
        payment.setSourceAccount("ACC-100");
        payment.setPayerUserId(1);

        when(paymentRepository.countRecentByAccountExcludingPayment(anyString(), any(LocalDateTime.class), eq(100)))
                .thenReturn(0L);
        when(paymentRepository.averageAmountByPayerUserIdExcluding(eq(1), eq(100)))
                .thenReturn(new BigDecimal("2000.00")); // 5000 is 2.5x, not 3x

        int score = service.scorePayment(payment, 100);

        assertEquals(0, score);
    }
}
