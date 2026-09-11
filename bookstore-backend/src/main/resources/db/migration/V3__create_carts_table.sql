-- One cart per guest token. No user_id yet
CREATE TABLE carts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_token VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
