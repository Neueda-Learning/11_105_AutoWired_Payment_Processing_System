package com.payment.server.repository;

import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.payment.server.model.BankAccount;

@Repository
public class BankAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public BankAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private BankAccount mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        BankAccount account = new BankAccount();
        account.setId(rs.getInt("id"));
        account.setUserId(rs.getInt("user_id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setIfscCode(rs.getString("ifsc_code"));
        account.setBankName(rs.getString("bank_name"));
        account.setBalance(rs.getBigDecimal("balance"));
        account.setPrimary(rs.getBoolean("is_primary"));
        account.setStatus(rs.getString("status"));
        return account;
    }

    public List<BankAccount> findAll() {
        return jdbcTemplate.query("SELECT * FROM bank_accounts", this::mapRow);
    }

    public List<BankAccount> findByUserId(int userId) {
        String sql = "SELECT * FROM bank_accounts WHERE user_id = ?";
        return jdbcTemplate.query(sql, this::mapRow, userId);
    }

    public BankAccount findById(int id) {
        String sql = "SELECT * FROM bank_accounts WHERE id = ?";
        List<BankAccount> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public BankAccount findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM bank_accounts WHERE account_number = ?";
        List<BankAccount> results = jdbcTemplate.query(sql, this::mapRow, accountNumber);
        return results.isEmpty() ? null : results.get(0);
    }

    public int save(BankAccount account) {
        String sql = "INSERT INTO bank_accounts "
                + "(user_id, account_number, ifsc_code, bank_name, balance, is_primary, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, account.getUserId());
            ps.setString(2, account.getAccountNumber());
            ps.setString(3, account.getIfscCode());
            ps.setString(4, account.getBankName());
            ps.setBigDecimal(5, account.getBalance());
            ps.setBoolean(6, account.isPrimary());
            ps.setString(7, account.getStatus());
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        account.setId(id);
        return id;
    }

    public void updateBalance(int id, java.math.BigDecimal newBalance) {
        jdbcTemplate.update("UPDATE bank_accounts SET balance = ? WHERE id = ?", newBalance, id);
    }

    public void updateStatus(int id, String status) {
        jdbcTemplate.update("UPDATE bank_accounts SET status = ? WHERE id = ?", status, id);
    }
}
