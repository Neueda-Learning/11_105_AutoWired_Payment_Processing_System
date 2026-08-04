-- Simple, plain SQL to create the tables needed by the app.
-- Runs automatically on startup (spring.sql.init.mode=always).

CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    country VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    source_account VARCHAR(50) NOT NULL,
    destination_account VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) UNIQUE,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10),
    payment_method VARCHAR(50),
    status VARCHAR(20),
    risk_score INT DEFAULT 0,
    reference VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS payment_status_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    status VARCHAR(20),
    previous_status VARCHAR(20),
    timestamp DATETIME,
    notes VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS error_codes (
    code VARCHAR(50) PRIMARY KEY,
    description VARCHAR(500),
    http_status INT,
    severity VARCHAR(20)
);
