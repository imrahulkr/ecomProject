-- 1) Indexes for lookups that ran as sequential scans. Postgres does not index foreign-key
--    columns automatically, and the legacy tables only ever had their primary keys indexed.
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_token_token_hash ON refresh_token (token_hash); -- every token refresh
CREATE INDEX IF NOT EXISTS idx_refresh_token_family_id ON refresh_token (family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id   ON refresh_token (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_email            ON orders (email);         -- customer order history
CREATE INDEX IF NOT EXISTS idx_orders_status           ON orders (order_status);  -- admin filter, expiry sweep
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id      ON cart_items (cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id   ON cart_items (product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id    ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id  ON order_items (product_id);
CREATE INDEX IF NOT EXISTS idx_products_category_id    ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_seller_id      ON products (seller_id);
CREATE INDEX IF NOT EXISTS idx_addresses_user_id       ON addresses (user_id);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_status_created ON payment_attempts (status, created_at); -- reconciliation sweep

-- 2) Unique constraints Hibernate (ddl-auto=update) added on top of the identical ones V14/V15
--    already define. Dropping a redundant duplicate loses nothing; no-ops on fresh databases.
ALTER TABLE reviews        DROP CONSTRAINT IF EXISTS uk1nv3auyahyyy79hvtrcqgtfo9;
ALTER TABLE wishlist_items DROP CONSTRAINT IF EXISTS uktp53unkks741xiqi6m620i7mx;

-- 3) Product soft delete (see Product.active).
ALTER TABLE products ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX idx_products_active ON products (active);

-- 4) Substring search on product name/description: trigram GIN indexes serve
--    lower(col) LIKE '%keyword%'. Creating an extension needs privileges some managed hosts
--    don't grant - in that case search still works, just without these indexes.
DO $$
BEGIN
    BEGIN
        CREATE EXTENSION IF NOT EXISTS pg_trgm;
    EXCEPTION WHEN insufficient_privilege OR feature_not_supported OR undefined_file THEN
        RAISE NOTICE 'pg_trgm unavailable - product search runs without trigram indexes';
    END;
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_products_name_trgm ON products USING gin (lower(product_name) gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_products_description_trgm ON products USING gin (lower(description) gin_trgm_ops)';
    END IF;
END $$;
