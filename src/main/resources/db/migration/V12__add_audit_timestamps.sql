-- Backfills created_at/updated_at (and a few partial gaps) across entities that were missing
-- them. DEFAULT now() backfills existing rows; going forward the entities' @PrePersist/@PreUpdate
-- callbacks set the real values on every write.

ALTER TABLE addresses ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE addresses ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE password_reset_tokens ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE verification_token ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE carts ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE carts ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE cart_items ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE cart_items ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE products ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE products ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE orders ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE orders ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE order_items ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE order_items ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE payments ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE payments ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();

-- Partial gaps: these tables already had one of the two columns.
ALTER TABLE stock_reservations ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE provider_health ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE refresh_token ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE email_logs ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();
