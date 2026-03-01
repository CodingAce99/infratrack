-- Schema for Infratrack database
-- Used with spring.sql.init.mode=always (demo/prod profiles)

CREATE TABLE IF NOT EXISTS assets (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    ip_address  VARCHAR(45)  NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    username    VARCHAR(255) NOT NULL,
    password    VARCHAR(500) NOT NULL  -- Encrypted with AES-256-GCM (IV + ciphertext in Base64)
);
