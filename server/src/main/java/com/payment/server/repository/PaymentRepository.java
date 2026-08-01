package com.payment.server.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Repository;

import com.payment.server.model.Payment;

@Repository
public class PaymentRepository {

    private final List<Payment> payments = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public List<Payment> findAll() {
        return new ArrayList<>(payments);
    }

    public Payment findById(int id) {
        return payments.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Payment> findByStatus(String status) {
        return payments.stream()
                .filter(p -> p.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    public int save(Payment payment) {
        int id = idCounter.incrementAndGet();
        payment.setId(id);
        payments.add(payment);
        return id;
    }

    public void updateStatus(int id, String status) {
        Payment payment = findById(id);
        if (payment != null) {
            payment.setStatus(status);
        }
    }

    public long countRecentByAccount(String sourceAccount, java.time.LocalDateTime since) {
        return payments.stream()
                .filter(p -> p.getSourceAccount().equalsIgnoreCase(sourceAccount))
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since))
                .count();
    }
}
