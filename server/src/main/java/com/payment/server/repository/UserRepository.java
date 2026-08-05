package com.payment.server.repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.payment.server.model.User;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private User mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPinHash(rs.getString("pin_hash"));
        user.setKycStatus(rs.getString("kyc_status"));
        user.setDailyLimit(rs.getBigDecimal("daily_limit"));
        user.setCountry(rs.getString("country"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        return user;
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT * FROM users", this::mapRow);
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        List<User> results = jdbcTemplate.query(sql, this::mapRow, email);
        return results.isEmpty() ? null : results.get(0);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0 : count;
    }

    public int save(User user) {
        String sql = "INSERT INTO users (full_name, email, phone, pin_hash, kyc_status, daily_limit, country, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getPinHash());
            ps.setString(5, user.getKycStatus());
            ps.setBigDecimal(6, user.getDailyLimit());
            ps.setString(7, user.getCountry());
            ps.setTimestamp(8, user.getCreatedAt() != null ? Timestamp.valueOf(user.getCreatedAt()) : null);
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        user.setId(id);
        return id;
    }

    public void updateKycStatus(int id, String kycStatus) {
        jdbcTemplate.update("UPDATE users SET kyc_status = ? WHERE id = ?", kycStatus, id);
    }

    public void updatePinHash(int id, String pinHash) {
        jdbcTemplate.update("UPDATE users SET pin_hash = ? WHERE id = ?", pinHash, id);
    }
}
