CREATE TABLE payment_attempts (
    id                          BIGSERIAL PRIMARY KEY,
    order_id                    BIGINT       NOT NULL REFERENCES orders (order_id),
    provider_name                VARCHAR(32)  NOT NULL,
    provider_payment_reference   VARCHAR(255),
    status                       VARCHAR(16)  NOT NULL,
    failure_reason                TEXT,
    amount_minor_units            BIGINT       NOT NULL,
    currency                      VARCHAR(8)   NOT NULL,
    created_at                    TIMESTAMP    NOT NULL,
    updated_at                    TIMESTAMP    NOT NULL
);

CREATE INDEX idx_payment_attempts_order_id ON payment_attempts (order_id);
CREATE INDEX idx_payment_attempts_provider_reference ON payment_attempts (provider_name, provider_payment_reference);
