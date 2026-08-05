package com.payment.server.repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.payment.server.model.TransactionFeeRule;

@Repository
public class TransactionFeeRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionFeeRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private TransactionFeeRule mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setId(rs.getInt("id"));
        rule.setPaymentMethod(rs.getString("payment_method"));
        rule.setMinAmount(rs.getBigDecimal("min_amount"));
        rule.setMaxAmount(rs.getBigDecimal("max_amount"));
        rule.setFeeType(rs.getString("fee_type"));
        rule.setFeeValue(rs.getBigDecimal("fee_value"));
        rule.setMinFeeCap(rs.getBigDecimal("min_fee_cap"));
        rule.setMaxFeeCap(rs.getBigDecimal("max_fee_cap"));
        Timestamp from = rs.getTimestamp("effective_from");
        if (from != null) {
            rule.setEffectiveFrom(from.toLocalDateTime());
        }
        Timestamp to = rs.getTimestamp("effective_to");
        if (to != null) {
            rule.setEffectiveTo(to.toLocalDateTime());
        }
        rule.setActive(rs.getBoolean("active"));
        return rule;
    }

    public List<TransactionFeeRule> findAll() {
        return jdbcTemplate.query("SELECT * FROM transaction_fee_rules ORDER BY id", this::mapRow);
    }

    public List<TransactionFeeRule> findActive() {
        return jdbcTemplate.query("SELECT * FROM transaction_fee_rules WHERE active = TRUE", this::mapRow);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transaction_fee_rules", Long.class);
        return count == null ? 0 : count;
    }

    public int save(TransactionFeeRule rule) {
        String sql = "INSERT INTO transaction_fee_rules "
                + "(payment_method, min_amount, max_amount, fee_type, fee_value, min_fee_cap, max_fee_cap, "
                + "effective_from, effective_to, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, rule.getPaymentMethod());
            ps.setBigDecimal(2, rule.getMinAmount());
            ps.setBigDecimal(3, rule.getMaxAmount());
            ps.setString(4, rule.getFeeType());
            ps.setBigDecimal(5, rule.getFeeValue());
            ps.setBigDecimal(6, rule.getMinFeeCap());
            ps.setBigDecimal(7, rule.getMaxFeeCap());
            ps.setTimestamp(8, rule.getEffectiveFrom() != null ? Timestamp.valueOf(rule.getEffectiveFrom()) : null);
            ps.setTimestamp(9, rule.getEffectiveTo() != null ? Timestamp.valueOf(rule.getEffectiveTo()) : null);
            ps.setBoolean(10, rule.isActive());
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        rule.setId(id);
        return id;
    }

    public TransactionFeeRule findById(int id) {
        String sql = "SELECT * FROM transaction_fee_rules WHERE id = ?";
        List<TransactionFeeRule> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public void update(TransactionFeeRule rule) {
        String sql = "UPDATE transaction_fee_rules SET payment_method = ?, min_amount = ?, max_amount = ?, "
                + "fee_type = ?, fee_value = ?, min_fee_cap = ?, max_fee_cap = ?, effective_from = ?, "
                + "effective_to = ?, active = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                rule.getPaymentMethod(),
                rule.getMinAmount(),
                rule.getMaxAmount(),
                rule.getFeeType(),
                rule.getFeeValue(),
                rule.getMinFeeCap(),
                rule.getMaxFeeCap(),
                rule.getEffectiveFrom() != null ? Timestamp.valueOf(rule.getEffectiveFrom()) : null,
                rule.getEffectiveTo() != null ? Timestamp.valueOf(rule.getEffectiveTo()) : null,
                rule.isActive(),
                rule.getId());
    }
}
