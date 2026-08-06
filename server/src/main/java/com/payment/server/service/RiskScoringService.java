package com.payment.server.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.payment.server.model.Payment;
import com.payment.server.repository.PaymentRepository;

@Service
public class RiskScoringService {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000");
    private static final int ODD_HOUR_START = 0;
    private static final int ODD_HOUR_END = 5;
    private static final int VELOCITY_WINDOW_MINUTES = 60;
    private static final int VELOCITY_THRESHOLD = 3;
    // Sudden-spike rule: flag if amount is >= 3x the payer's rolling average.
    private static final BigDecimal SPIKE_MULTIPLIER = new BigDecimal("3");

    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Autowired
    public RiskScoringService(PaymentRepository paymentRepository) {
        this(paymentRepository, Clock.systemUTC());
    }

    public RiskScoringService(PaymentRepository paymentRepository, Clock clock) {
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    public int scorePayment(Payment payment, int currentPaymentId) {
        int score = 0;

        // High amount rule
        if (payment.getAmount() != null && payment.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            score += 40;
        }

        // Odd hours rule
        int hour = LocalDateTime.now(clock).getHour();
        if (hour >= ODD_HOUR_START && hour <= ODD_HOUR_END) {
            score += 20;
        }

        // Velocity rule - recent transaction count from same source account
        LocalDateTime since = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        long recentCount = paymentRepository.countRecentByAccountExcludingPayment(
                payment.getSourceAccount(), since, currentPaymentId);
        if (recentCount >= VELOCITY_THRESHOLD) {
            score += 40;
        }

        // Sudden increase in transaction value rule - see
        // payment-system-v2-design.md section 9. Compares the current
        // amount to the payer's historical rolling average; only applies
        // when linked to a platform User (payerUserId present).
        if (payment.getPayerUserId() != null && payment.getAmount() != null) {
            BigDecimal average = paymentRepository.averageAmountByPayerUserIdExcluding(
                    payment.getPayerUserId(), currentPaymentId);
            if (average != null && average.compareTo(BigDecimal.ZERO) > 0
                    && payment.getAmount().compareTo(average.multiply(SPIKE_MULTIPLIER)) >= 0) {
                score += 30;
            }
        }

        return Math.min(score, 100);
    }
}
