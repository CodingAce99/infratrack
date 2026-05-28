-- V2: Add users table for authentication
-- Sprint 7.1 — Phase 7 (Auth & Authorization) foundation

CREATE TABLE users (
    id            VARCHAR(36) PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(72) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'VIEWER'))
);