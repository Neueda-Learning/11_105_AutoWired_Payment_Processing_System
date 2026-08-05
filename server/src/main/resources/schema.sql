-- Simple, plain SQL to create the tables needed by the app.
-- Runs automatically on startup (spring.sql.init.mode=always).

CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    country VARCHAR(10)
);

-- Identity: a person with an account at the bank (payer or payee).
-- See new-docs/payment-system-v2-design.md, section 3.
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    pin_hash VARCHAR(255),
    kyc_status VARCHAR(20) DEFAULT 'PENDING',
    daily_limit DECIMAL(19, 2),
    country VARCHAR(10),
    created_at DATETIME
);

-- A bank account belonging to a User; simulated ledger balance.
CREATE TABLE IF NOT EXISTS bank_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    ifsc_code VARCHAR(20),
    bank_name VARCHAR(100),
    balance DECIMAL(19, 2) DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT fk_bank_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- A route (UPI/card/net-banking) used to initiate payments from a BankAccount.
CREATE TABLE IF NOT EXISTS payment_methods (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    bank_account_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    upi_id VARCHAR(255),
    card_last4 VARCHAR(4),
    card_token VARCHAR(255),
    card_expiry VARCHAR(7),
    card_holder_name VARCHAR(255),
    linked_bank_name VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_payment_methods_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_payment_methods_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id)
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
    card_last4 VARCHAR(4),
    card_expiry VARCHAR(7),
    card_holder_name VARCHAR(255),
    upi_id VARCHAR(255),
    bank_name VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME
);

-- Note: CREATE TABLE IF NOT EXISTS does not add columns to a table that
-- already exists. For existing databases, the credit card columns are
-- added by SchemaMigrationRunner at startup (this MySQL version doesn't
-- support ALTER TABLE ... ADD COLUMN IF NOT EXISTS).

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

-- Admin-managed dynamic fee rules (see new-docs/payment-system-v2-design.md).
CREATE TABLE IF NOT EXISTS transaction_fee_rules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    payment_method VARCHAR(50) NOT NULL,
    min_amount DECIMAL(19, 2) NOT NULL,
    max_amount DECIMAL(19, 2),
    fee_type VARCHAR(20) NOT NULL,
    fee_value DECIMAL(10, 4) NOT NULL,
    min_fee_cap DECIMAL(19, 2),
    max_fee_cap DECIMAL(19, 2),
    effective_from DATETIME,
    effective_to DATETIME,
    active BOOLEAN DEFAULT TRUE
);

-- PIN/OTP authentication challenge issued for a payment before validation.
-- See new-docs/payment-system-v2-design.md, section 5.
CREATE TABLE IF NOT EXISTS auth_challenges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT NOT NULL,
    method VARCHAR(10) NOT NULL,
    code_hash VARCHAR(255),
    expires_at DATETIME,
    attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    status VARCHAR(20) DEFAULT 'PENDING',
    CONSTRAINT fk_auth_challenges_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);
