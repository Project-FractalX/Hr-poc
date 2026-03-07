-- FractalX Saga Orchestrator — Initial Schema
CREATE TABLE IF NOT EXISTS saga_instance (
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    saga_id                      VARCHAR(255) NOT NULL,
    correlation_id               VARCHAR(36)  NOT NULL UNIQUE,
    owner_service                VARCHAR(255),
    status                       VARCHAR(50)  NOT NULL,
    current_step                 VARCHAR(255),
    payload                      TEXT,
    error_message                TEXT,
    started_at                   TIMESTAMP    NOT NULL,
    updated_at                   TIMESTAMP,
    owner_notified               BOOLEAN      NOT NULL DEFAULT FALSE,
    notification_retry_count     INT          NOT NULL DEFAULT 0,
    last_notification_attempt    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_saga_status          ON saga_instance (status);
CREATE INDEX IF NOT EXISTS idx_saga_id              ON saga_instance (saga_id);
CREATE INDEX IF NOT EXISTS idx_saga_corr            ON saga_instance (correlation_id);
CREATE INDEX IF NOT EXISTS idx_saga_notify_pending  ON saga_instance (owner_notified, status);
