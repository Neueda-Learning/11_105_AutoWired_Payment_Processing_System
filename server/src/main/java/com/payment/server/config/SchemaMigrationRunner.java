package com.payment.server.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds columns to existing tables that schema.sql's
 * {@code CREATE TABLE IF NOT EXISTS} cannot add once a table already exists
 * (e.g. idempotency_key, and the credit card / UPI / net banking columns).
 * Uses information_schema checks + plain ALTER TABLE (no
 * {@code IF NOT EXISTS} clause) for compatibility with older MySQL versions
 * that don't support it on ADD COLUMN.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("payments", "idempotency_key", "VARCHAR(100) UNIQUE");
        addColumnIfMissing("payments", "card_last4", "VARCHAR(4)");
        addColumnIfMissing("payments", "card_expiry", "VARCHAR(7)");
        addColumnIfMissing("payments", "upi_id", "VARCHAR(255)");
        addColumnIfMissing("payments", "bank_name", "VARCHAR(100)");
        addColumnIfMissing("payments", "fee_amount", "DECIMAL(19,2)");
        addColumnIfMissing("payments", "fee_percentage", "DECIMAL(10,4)");
        addColumnIfMissing("payments", "net_amount", "DECIMAL(19,2)");
        addColumnIfMissing("payments", "payer_user_id", "INT");
        addColumnIfMissing("payments", "payee_user_id", "INT");
        addColumnIfMissing("payments", "source_payment_method_id", "INT");
        addColumnIfMissing("payments", "gross_amount", "DECIMAL(19,2)");
        addColumnIfMissing("payments", "authentication_status", "VARCHAR(20)");
        addColumnIfMissing("users", "daily_limit", "DECIMAL(19,2)");
        addColumnIfMissing("users", "country", "VARCHAR(10)");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);

        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
