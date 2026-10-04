-- 1) Refunds for cancelled/returned items -------------------------------------------------------

-- The id a refund must target: the Stripe PaymentIntent, or Razorpay's captured payment (pay_...)
-- whose order id is all provider_payment_reference holds. Recorded when the payment succeeds.
ALTER TABLE payment_attempts ADD COLUMN provider_payment_id VARCHAR(255);

ALTER TABLE order_items ADD COLUMN return_reason        TEXT;
ALTER TABLE order_items ADD COLUMN return_requested_at  TIMESTAMP;
ALTER TABLE order_items ADD COLUMN refund_status        VARCHAR(16);
ALTER TABLE order_items ADD COLUMN refunded_minor_units BIGINT NOT NULL DEFAULT 0;

CREATE TABLE refunds (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT       NOT NULL REFERENCES orders (order_id),
    order_item_id      BIGINT       NOT NULL REFERENCES order_items (order_item_id),
    payment_attempt_id BIGINT       NOT NULL REFERENCES payment_attempts (id),
    provider_name      VARCHAR(32)  NOT NULL,
    provider_refund_id VARCHAR(255),
    amount_minor_units BIGINT       NOT NULL,
    currency           VARCHAR(8)   NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    reason             TEXT,
    attempts           INTEGER      NOT NULL DEFAULT 0,
    last_error         TEXT,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_refunds_order_item UNIQUE (order_item_id)
);
CREATE INDEX idx_refunds_order_id ON refunds (order_id);
CREATE INDEX idx_refunds_status   ON refunds (status);

-- 2) Shipping fee charged on the order ------------------------------------------------------------
ALTER TABLE orders ADD COLUMN shipping_minor_units BIGINT NOT NULL DEFAULT 0;

-- 3) Seller earnings ledger and payouts -----------------------------------------------------------
CREATE TABLE seller_payouts (
    id                 BIGSERIAL PRIMARY KEY,
    seller_id          BIGINT       NOT NULL REFERENCES users (user_id),
    amount_minor_units BIGINT       NOT NULL,
    currency           VARCHAR(8)   NOT NULL,
    reference          VARCHAR(255),
    created_by         BIGINT       REFERENCES users (user_id),
    created_at         TIMESTAMP    NOT NULL
);
CREATE INDEX idx_seller_payouts_seller_id ON seller_payouts (seller_id);

-- Signed amounts: SALE (+), COMMISSION (-), REFUND (-), COMMISSION_REVERSAL (+). An entry is
-- paid out once payout_id is set.
CREATE TABLE seller_ledger_entries (
    id                 BIGSERIAL PRIMARY KEY,
    seller_id          BIGINT       NOT NULL REFERENCES users (user_id),
    order_id           BIGINT       NOT NULL REFERENCES orders (order_id),
    order_item_id      BIGINT       NOT NULL REFERENCES order_items (order_item_id),
    entry_type         VARCHAR(32)  NOT NULL,
    amount_minor_units BIGINT       NOT NULL,
    currency           VARCHAR(8)   NOT NULL,
    payout_id          BIGINT       REFERENCES seller_payouts (id),
    created_at         TIMESTAMP    NOT NULL
);
CREATE INDEX idx_seller_ledger_seller_payout ON seller_ledger_entries (seller_id, payout_id);
CREATE INDEX idx_seller_ledger_order_item    ON seller_ledger_entries (order_item_id);

-- Backfill earnings for orders that were already paid, using the default 10% commission
-- (app.marketplace.commission-percent). Orders paid from now on are recorded by the app.
INSERT INTO seller_ledger_entries (seller_id, order_id, order_item_id, entry_type, amount_minor_units, currency, created_at)
SELECT oi.seller_id, o.order_id, oi.order_item_id, 'SALE', oi.ordered_product_price_minor_units * oi.quantity, oi.currency, now()
FROM order_items oi JOIN orders o ON o.order_id = oi.order_id
WHERE o.order_status = 'PAID' AND oi.seller_id IS NOT NULL;

INSERT INTO seller_ledger_entries (seller_id, order_id, order_item_id, entry_type, amount_minor_units, currency, created_at)
SELECT oi.seller_id, o.order_id, oi.order_item_id, 'COMMISSION', -ROUND(oi.ordered_product_price_minor_units * oi.quantity * 10 / 100.0), oi.currency, now()
FROM order_items oi JOIN orders o ON o.order_id = oi.order_id
WHERE o.order_status = 'PAID' AND oi.seller_id IS NOT NULL;
