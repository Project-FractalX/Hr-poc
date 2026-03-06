-- ================================================================
-- FractalX Admin Service — Initial Schema                  V1
-- Compatible with: MySQL 8+, PostgreSQL 15+, H2 2.x
-- ================================================================

-- Admin users (username is the PK / natural key)
CREATE TABLE IF NOT EXISTS admin_users (
    username        VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      VARCHAR(50),
    last_login_at   VARCHAR(50),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (username)
);

-- User roles — one-to-many via @ElementCollection
CREATE TABLE IF NOT EXISTS admin_user_roles (
    username  VARCHAR(100) NOT NULL,
    role      VARCHAR(50)  NOT NULL,
    PRIMARY KEY (username, role),
    FOREIGN KEY (username)
        REFERENCES admin_users(username)
        ON DELETE CASCADE
);

-- Admin settings singleton (always id = 1)
CREATE TABLE IF NOT EXISTS admin_settings (
    id                  INT          NOT NULL,
    site_name           VARCHAR(255) NOT NULL DEFAULT 'FractalX Admin',
    theme               VARCHAR(50)  NOT NULL DEFAULT 'light',
    session_timeout_min INT          NOT NULL DEFAULT 30,
    default_alert_email VARCHAR(255) NOT NULL DEFAULT '',
    maintenance_mode    BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);
