package com.payment.server.repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.payment.server.model.AuthChallenge;

@Repository
public class AuthChallengeRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthChallengeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private AuthChallenge mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        AuthChallenge challenge = new AuthChallenge();
        challenge.setId(rs.getInt("id"));
        challenge.setPaymentId(rs.getInt("payment_id"));
        challenge.setMethod(rs.getString("method"));
        challenge.setCodeHash(rs.getString("code_hash"));
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            challenge.setExpiresAt(expiresAt.toLocalDateTime());
        }
        challenge.setAttempts(rs.getInt("attempts"));
        challenge.setMaxAttempts(rs.getInt("max_attempts"));
        challenge.setStatus(rs.getString("status"));
        return challenge;
    }

    public AuthChallenge findById(int id) {
        String sql = "SELECT * FROM auth_challenges WHERE id = ?";
        List<AuthChallenge> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public AuthChallenge findLatestByPaymentId(int paymentId) {
        String sql = "SELECT * FROM auth_challenges WHERE payment_id = ? ORDER BY id DESC LIMIT 1";
        List<AuthChallenge> results = jdbcTemplate.query(sql, this::mapRow, paymentId);
        return results.isEmpty() ? null : results.get(0);
    }

    public long countByPaymentId(int paymentId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_challenges WHERE payment_id = ?", Long.class, paymentId);
        return count == null ? 0 : count;
    }

    public int save(AuthChallenge challenge) {
        String sql = "INSERT INTO auth_challenges "
                + "(payment_id, method, code_hash, expires_at, attempts, max_attempts, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, challenge.getPaymentId());
            ps.setString(2, challenge.getMethod());
            ps.setString(3, challenge.getCodeHash());
            ps.setTimestamp(4, challenge.getExpiresAt() != null ? Timestamp.valueOf(challenge.getExpiresAt()) : null);
            ps.setInt(5, challenge.getAttempts());
            ps.setInt(6, challenge.getMaxAttempts());
            ps.setString(7, challenge.getStatus());
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        challenge.setId(id);
        return id;
    }

    public void incrementAttempts(int id) {
        jdbcTemplate.update("UPDATE auth_challenges SET attempts = attempts + 1 WHERE id = ?", id);
    }

    public void updateStatus(int id, String status) {
        jdbcTemplate.update("UPDATE auth_challenges SET status = ? WHERE id = ?", status, id);
    }
}
