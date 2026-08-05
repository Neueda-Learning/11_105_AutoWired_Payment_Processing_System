package com.payment.server.repository;

import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.payment.server.model.PaymentMethod;

@Repository
public class PaymentMethodRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentMethodRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private PaymentMethod mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        PaymentMethod method = new PaymentMethod();
        method.setId(rs.getInt("id"));
        method.setUserId(rs.getInt("user_id"));
        method.setBankAccountId(rs.getInt("bank_account_id"));
        method.setType(rs.getString("type"));
        method.setUpiId(rs.getString("upi_id"));
        method.setCardLast4(rs.getString("card_last4"));
        method.setCardToken(rs.getString("card_token"));
        method.setCardExpiry(rs.getString("card_expiry"));
        method.setCardHolderName(rs.getString("card_holder_name"));
        method.setLinkedBankName(rs.getString("linked_bank_name"));
        method.setDefault(rs.getBoolean("is_default"));
        return method;
    }

    public List<PaymentMethod> findByUserId(int userId) {
        String sql = "SELECT * FROM payment_methods WHERE user_id = ?";
        return jdbcTemplate.query(sql, this::mapRow, userId);
    }

    public PaymentMethod findById(int id) {
        String sql = "SELECT * FROM payment_methods WHERE id = ?";
        List<PaymentMethod> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public int save(PaymentMethod method) {
        String sql = "INSERT INTO payment_methods "
                + "(user_id, bank_account_id, type, upi_id, card_last4, card_token, card_expiry, card_holder_name, linked_bank_name, is_default) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, method.getUserId());
            ps.setInt(2, method.getBankAccountId());
            ps.setString(3, method.getType());
            ps.setString(4, method.getUpiId());
            ps.setString(5, method.getCardLast4());
            ps.setString(6, method.getCardToken());
            ps.setString(7, method.getCardExpiry());
            ps.setString(8, method.getCardHolderName());
            ps.setString(9, method.getLinkedBankName());
            ps.setBoolean(10, method.isDefault());
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        method.setId(id);
        return id;
    }

    public void clearDefaultForUser(int userId) {
        jdbcTemplate.update("UPDATE payment_methods SET is_default = FALSE WHERE user_id = ?", userId);
    }

    public void update(PaymentMethod method) {
        String sql = "UPDATE payment_methods SET upi_id = ?, card_last4 = ?, card_token = ?, "
                + "card_expiry = ?, card_holder_name = ?, linked_bank_name = ?, is_default = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                method.getUpiId(),
                method.getCardLast4(),
                method.getCardToken(),
                method.getCardExpiry(),
                method.getCardHolderName(),
                method.getLinkedBankName(),
                method.isDefault(),
                method.getId());
    }

    public void deleteById(int id) {
        jdbcTemplate.update("DELETE FROM payment_methods WHERE id = ?", id);
    }
}
