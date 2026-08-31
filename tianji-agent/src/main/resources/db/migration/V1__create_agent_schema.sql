CREATE TABLE ai_conversation (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    scene VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    summary LONGTEXT NULL,
    prompt_version VARCHAR(32) NOT NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_conversation_user_update (user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content LONGTEXT NOT NULL,
    model VARCHAR(80) NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    latency_ms BIGINT NULL,
    finish_reason VARCHAR(32) NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_message_conversation_time (conversation_id, create_time),
    INDEX idx_message_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_citation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    chunk_id VARCHAR(36) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(80) NULL,
    course_id BIGINT NULL,
    chapter_id BIGINT NULL,
    section_id BIGINT NULL,
    start_moment INT NULL,
    end_moment INT NULL,
    title VARCHAR(300) NOT NULL,
    score DOUBLE NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_citation_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_tool_call (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    tool_name VARCHAR(80) NOT NULL,
    arguments_digest VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    latency_ms BIGINT NULL,
    error_code VARCHAR(64) NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_tool_message (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_pending_action (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    payload LONGTEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    expire_time DATETIME(6) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    result_message VARCHAR(300) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    UNIQUE KEY uk_pending_action_idempotency (idempotency_key),
    INDEX idx_pending_action_user (user_id, status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message_id VARCHAR(36) NOT NULL,
    rating VARCHAR(16) NOT NULL,
    reason VARCHAR(64) NULL,
    comment VARCHAR(500) NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    UNIQUE KEY uk_feedback_user_message (user_id, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_knowledge_document (
    id VARCHAR(36) PRIMARY KEY,
    course_id BIGINT NULL,
    chapter_id BIGINT NULL,
    section_id BIGINT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(80) NULL,
    title VARCHAR(300) NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    visibility VARCHAR(16) NOT NULL,
    source_url VARCHAR(500) NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_knowledge_course_status (course_id, status),
    INDEX idx_knowledge_dedup (course_id, source_type, source_id, content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_knowledge_chunk (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    course_id BIGINT NULL,
    chapter_id BIGINT NULL,
    section_id BIGINT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(80) NULL,
    title VARCHAR(300) NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    start_moment INT NULL,
    end_moment INT NULL,
    chunk_index INT NULL,
    active BIT(1) NOT NULL,
    embedding_model VARCHAR(80) NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_chunk_document (document_id),
    INDEX idx_chunk_course_active (course_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_model_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    scene VARCHAR(32) NOT NULL,
    model VARCHAR(80) NOT NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    latency_ms BIGINT NULL,
    estimated_cost_micros BIGINT NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    INDEX idx_usage_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_prompt_version (
    id VARCHAR(36) PRIMARY KEY,
    prompt_key VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    publisher_id BIGINT NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    UNIQUE KEY uk_prompt_key_version (prompt_key, version),
    INDEX idx_prompt_active (prompt_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
