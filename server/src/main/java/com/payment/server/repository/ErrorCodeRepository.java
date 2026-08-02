package com.payment.server.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.payment.server.model.ErrorCode;

@Repository
public class ErrorCodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public ErrorCodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private ErrorCode mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ErrorCode(
                rs.getString("code"),
                rs.getString("description"),
                rs.getInt("http_status"),
                rs.getString("severity"));
    }

    public List<ErrorCode> findAll() {
        String sql = "SELECT * FROM error_codes";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    public ErrorCode findByCode(String code) {
        String sql = "SELECT * FROM error_codes WHERE code = ?";
        List<ErrorCode> results = jdbcTemplate.query(sql, this::mapRow, code);
        return results.isEmpty() ? null : results.get(0);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM error_codes";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    public void save(ErrorCode errorCode) {
        String sql = "INSERT INTO error_codes (code, description, http_status, severity) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, errorCode.getCode(), errorCode.getDescription(),
                errorCode.getHttpStatus(), errorCode.getSeverity());
    }
}
