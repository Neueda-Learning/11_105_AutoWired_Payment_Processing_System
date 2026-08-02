package com.payment.server.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.payment.server.model.Customer;

@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Customer mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("account_number"),
                rs.getString("country"));
    }

    public List<Customer> findAll() {
        String sql = "SELECT * FROM customers";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    public Customer findById(int id) {
        String sql = "SELECT * FROM customers WHERE id = ?";
        List<Customer> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.isEmpty() ? null : results.get(0);
    }

    public Customer findByAccountNumber(String accountNumber) {
        String sql = "SELECT * FROM customers WHERE account_number = ?";
        List<Customer> results = jdbcTemplate.query(sql, this::mapRow, accountNumber);
        return results.isEmpty() ? null : results.get(0);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM customers";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    public void save(Customer customer) {
        String sql = "INSERT INTO customers (name, account_number, country) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, customer.getName(), customer.getAccountNumber(), customer.getCountry());
    }
}
