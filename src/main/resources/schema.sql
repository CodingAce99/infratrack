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

CREATE TABLE IF NOT EXISTS metrics (
    id           VARCHAR(36) PRIMARY KEY,
    asset_id     VARCHAR(36) NOT NULL,
    cpu_usage    DOUBLE PRECISION NOT NULL,
    memory_usage DOUBLE PRECISION NOT NULL,
    disk_usage   DOUBLE PRECISION NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_metrics_asset FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);

CREATE INDEX idx_metrics_asset_time ON metrics(asset_id, collected_at DESC);
