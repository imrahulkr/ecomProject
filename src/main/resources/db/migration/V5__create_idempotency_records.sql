CREATE TABLE idempotency_records (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    idempotency_key   VARCHAR(255) NOT NULL,
    endpoint          VARCHAR(32)  NOT NULL,
    request_hash      VARCHAR(64)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    order_id          BIGINT,
    response_body     TEXT,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    CONSTRAINT uq_idempotency_user_key_endpoint UNIQUE (user_id, idempotency_key, endpoint)
);
