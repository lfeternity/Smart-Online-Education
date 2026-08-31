CREATE TABLE ai_user_profile (
    user_id BIGINT PRIMARY KEY,
    learning_goal VARCHAR(500) NULL,
    preferred_style VARCHAR(32) NULL,
    weekly_hours INT NULL,
    consented BIT(1) NOT NULL DEFAULT b'0',
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_ingestion_job (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    requested_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    stage VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    next_attempt_time DATETIME(6) NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(500) NULL,
    started_time DATETIME(6) NULL,
    completed_time DATETIME(6) NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_ingestion_status_retry (status, next_attempt_time),
    INDEX idx_ingestion_document (document_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE ai_model_usage
    ADD COLUMN price_version VARCHAR(32) NOT NULL DEFAULT 'unpriced' AFTER estimated_cost_micros;
