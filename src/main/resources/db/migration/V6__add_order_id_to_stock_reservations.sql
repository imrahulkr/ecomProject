ALTER TABLE stock_reservations ADD COLUMN order_id BIGINT NULL REFERENCES orders (order_id);

CREATE INDEX idx_stock_reservations_order_id ON stock_reservations (order_id);
