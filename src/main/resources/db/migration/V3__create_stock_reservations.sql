CREATE TABLE stock_reservations (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES products (product_id),
    cart_id     BIGINT       NOT NULL REFERENCES carts (cart_id),
    quantity    INTEGER      NOT NULL,
    status      VARCHAR(16)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_reservations_status_expires_at ON stock_reservations (status, expires_at);
