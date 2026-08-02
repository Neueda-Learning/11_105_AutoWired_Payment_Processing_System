package com.payment.server.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.payment.server.model.PaymentStatusHistory;

@Repository
public class PaymentStatusHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentStatusHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private PaymentStatusHistory mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        PaymentStatusHistory entry = new PaymentStatusHistory();
        entry.setId(rs.getInt("id"));
        entry.setPaymentId(rs.getInt("payment_id"));
        entry.setStatus(rs.getString("status"));
        entry.setPreviousStatus(rs.getString("previous_status"));
        java.sql.Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            entry.setTimestamp(timestamp.toLocalDateTime());
        }
        entry.setNotes(rs.getString("notes"));
        return entry;
    }

    public List<PaymentStatusHistory> findByPaymentId(int paymentId) {
        String sql = "SELECT * FROM payment_status_history WHERE payment_id = ?";
        return jdbcTemplate.query(sql, this::mapRow, paymentId);
    }

    public void save(int paymentId, String status, String previousStatus, String notes) {
        String sql = "INSERT INTO payment_status_history "
                + "(payment_id, status, previous_status, timestamp, notes) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, paymentId, status, previousStatus,
                java.sql.Timestamp.valueOf(LocalDateTime.now()), notes);
    }
}
