ALTER TABLE order_items ADD COLUMN seller_id BIGINT REFERENCES users (user_id);
ALTER TABLE order_items ADD COLUMN fulfillment_status VARCHAR(16) NOT NULL DEFAULT 'PENDING';
ALTER TABLE order_items ADD COLUMN tracking_number VARCHAR(255);
ALTER TABLE order_items ADD COLUMN carrier VARCHAR(255);
ALTER TABLE order_items ADD COLUMN shipped_at TIMESTAMP;
ALTER TABLE order_items ADD COLUMN delivered_at TIMESTAMP;

-- Backfill seller_id for pre-existing rows from the product's current owner - matches the
-- products.seller_id FK naming already used for that same relationship.
UPDATE order_items oi SET seller_id = p.seller_id
    FROM products p WHERE oi.product_id = p.product_id AND oi.seller_id IS NULL;

CREATE INDEX idx_order_items_seller_id ON order_items (seller_id);
