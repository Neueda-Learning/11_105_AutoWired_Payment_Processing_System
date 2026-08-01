package com.payment.server.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private final PaymentRepository paymentRepository;

    public RiskScoringService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public int scorePayment(Payment payment) {
        int score = 0;

        // High amount rule
        if (payment.getAmount() != null && payment.getAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            score += 40;
        }

        // Odd hours rule
        int hour = LocalDateTime.now().getHour();
        if (hour >= ODD_HOUR_START && hour <= ODD_HOUR_END) {
            score += 20;
        }

        // Velocity rule - recent transaction count from same source account
        LocalDateTime since = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        long recentCount = paymentRepository.countRecentByAccount(payment.getSourceAccount(), since);
        if (recentCount >= VELOCITY_THRESHOLD) {
            score += 40;
        }

        return Math.min(score, 100);
    }
}
