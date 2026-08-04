package com.payment.server.repository;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.payment.server.model.Payment;

@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Payment mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        payment.setUserId(rs.wasNull() ? null : userId);
        payment.setSourceAccount(rs.getString("source_account"));
        payment.setDestinationAccount(rs.getString("destination_account"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setCurrency(rs.getString("currency"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setStatus(rs.getString("status"));
        payment.setRiskScore(rs.getInt("risk_score"));
        payment.setReference(rs.getString("reference"));
        payment.setIdempotencyKey(rs.getString("idempotency_key"));
        payment.setCardLast4(rs.getString("card_last4"));
        payment.setCardExpiry(rs.getString("card_expiry"));
        payment.setUpiId(rs.getString("upi_id"));
        payment.setBankName(rs.getString("bank_name"));
        payment.setProcessingFee(rs.getBigDecimal("processing_fee"));
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            payment.setCreatedAt(createdAt.toLocalDateTime());
        }
        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            payment.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return payment;
    }

    public List<Payment> findAll() {
        String sql = "SELECT * FROM payments";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    public Payment findById(int id) {
        String sql = "SELECT * FROM payments WHERE id = ?";
        List<Payment> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Payment> findByStatus(String status) {
        String sql = "SELECT * FROM payments WHERE status = ?";
        return jdbcTemplate.query(sql, this::mapRow, status);
    }

    public List<Payment> findByUserId(int userId) {
        String sql = "SELECT * FROM payments WHERE user_id = ?";
        return jdbcTemplate.query(sql, this::mapRow, userId);
    }

    public List<Payment> findByUserIdAndStatus(int userId, String status) {
        String sql = "SELECT * FROM payments WHERE user_id = ? AND status = ?";
        return jdbcTemplate.query(sql, this::mapRow, userId, status);
    }

    public Payment findByIdempotencyKey(String key) {
        String sql = "SELECT * FROM payments WHERE idempotency_key = ?";
        List<Payment> results = jdbcTemplate.query(sql, this::mapRow, key);
        return results.isEmpty() ? null : results.get(0);
    }

    public int save(Payment payment) {
        String sql = "INSERT INTO payments "
                + "(user_id, source_account, destination_account, idempotency_key, amount, currency, payment_method, "
                + "status, risk_score, reference, card_last4, card_expiry, upi_id, bank_name, processing_fee, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (payment.getUserId() != null) {
                ps.setInt(1, payment.getUserId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, payment.getSourceAccount());
            ps.setString(3, payment.getDestinationAccount());
            ps.setString(4, payment.getIdempotencyKey());
            ps.setBigDecimal(5, payment.getAmount());
            ps.setString(6, payment.getCurrency());
            ps.setString(7, payment.getPaymentMethod());
            ps.setString(8, payment.getStatus());
            ps.setInt(9, payment.getRiskScore());
            ps.setString(10, payment.getReference());
            ps.setString(11, payment.getCardLast4());
            ps.setString(12, payment.getCardExpiry());
            ps.setString(13, payment.getUpiId());
            ps.setString(14, payment.getBankName());
            ps.setBigDecimal(15, payment.getProcessingFee());
            ps.setTimestamp(16, java.sql.Timestamp.valueOf(payment.getCreatedAt()));
            ps.setTimestamp(17, java.sql.Timestamp.valueOf(payment.getUpdatedAt()));
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        payment.setId(id);
        return id;
    }

    public void updateStatus(int id, String status) {
        String sql = "UPDATE payments SET status = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, java.sql.Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public void updateRiskScore(int id, int riskScore) {
        String sql = "UPDATE payments SET risk_score = ? WHERE id = ?";
        jdbcTemplate.update(sql, riskScore, id);
    }

    public long countAll() {
        String sql = "SELECT COUNT(*) FROM payments";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    public java.math.BigDecimal sumAmountAll() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments";
        return jdbcTemplate.queryForObject(sql, java.math.BigDecimal.class);
    }

    public java.math.BigDecimal sumFeeByStatus(String status) {
        String sql = "SELECT COALESCE(SUM(processing_fee), 0) FROM payments WHERE status = ?";
        return jdbcTemplate.queryForObject(sql, java.math.BigDecimal.class, status);
    }

    public long countRecentByAccount(String sourceAccount, LocalDateTime since) {
        String sql = "SELECT COUNT(*) FROM payments WHERE source_account = ? AND created_at > ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, sourceAccount, java.sql.Timestamp.valueOf(since));
        return count == null ? 0 : count;
    }

    public long countRecentByAccountExcludingPayment(String sourceAccount, LocalDateTime since, int excludedPaymentId) {
        String sql = "SELECT COUNT(*) FROM payments WHERE source_account = ? AND created_at > ? AND id <> ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class,
                sourceAccount, java.sql.Timestamp.valueOf(since), excludedPaymentId);
        return count == null ? 0 : count;
    }
}
