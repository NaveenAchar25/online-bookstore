CREATE TABLE users (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                  VARCHAR(255)  NOT NULL UNIQUE,
    password_hash          VARCHAR(255)  NOT NULL,
    first_name             VARCHAR(100)  NOT NULL,
    last_name              VARCHAR(100)  NOT NULL,
    failed_login_attempts  INT           NOT NULL DEFAULT 0,
    locked_until           TIMESTAMP     NULL,
    enabled                BOOLEAN       NOT NULL DEFAULT TRUE,
    must_change_password   BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at             TIMESTAMP     NULL,
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
