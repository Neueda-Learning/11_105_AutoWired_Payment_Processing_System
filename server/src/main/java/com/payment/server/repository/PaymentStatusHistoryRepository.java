package com.payment.server.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Repository;

import com.payment.server.model.PaymentStatusHistory;

@Repository
public class PaymentStatusHistoryRepository {

    private final List<PaymentStatusHistory> history = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public List<PaymentStatusHistory> findByPaymentId(int paymentId) {
        return history.stream()
                .filter(h -> h.getPaymentId() == paymentId)
                .toList();
    }

    public PaymentStatusHistory save(int paymentId, String status, String previousStatus, String notes) {
        PaymentStatusHistory entry = new PaymentStatusHistory(
                idCounter.incrementAndGet(),
                paymentId,
                status,
                previousStatus,
                LocalDateTime.now(),
                notes);
        history.add(entry);
        return entry;
    }
}
