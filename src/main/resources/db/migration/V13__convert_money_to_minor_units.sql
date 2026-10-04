-- Converts floating-point money columns (DOUBLE PRECISION, imprecise for currency) to integer minor
-- units (BIGINT, e.g. paise for INR) plus an explicit currency column, mirroring the pattern already
-- used by payment_attempts.amount_minor_units/currency. discount columns are left untouched - they
-- store a percentage, not a currency amount.

-- products
ALTER TABLE products ADD COLUMN price_minor_units BIGINT;
ALTER TABLE products ADD COLUMN special_price_minor_units BIGINT;
ALTER TABLE products ADD COLUMN currency VARCHAR(8);
UPDATE products SET price_minor_units = ROUND(COALESCE(price, 0) * 100),
                     special_price_minor_units = ROUND(COALESCE(special_price, 0) * 100),
                     currency = 'INR';
ALTER TABLE products ALTER COLUMN price_minor_units SET NOT NULL;
ALTER TABLE products ALTER COLUMN special_price_minor_units SET NOT NULL;
ALTER TABLE products ALTER COLUMN currency SET NOT NULL;
ALTER TABLE products DROP COLUMN price;
ALTER TABLE products DROP COLUMN special_price;

-- cart_items
ALTER TABLE cart_items ADD COLUMN product_price_minor_units BIGINT;
ALTER TABLE cart_items ADD COLUMN currency VARCHAR(8);
UPDATE cart_items SET product_price_minor_units = ROUND(COALESCE(product_price, 0) * 100),
                       currency = 'INR';
ALTER TABLE cart_items ALTER COLUMN product_price_minor_units SET NOT NULL;
ALTER TABLE cart_items ALTER COLUMN currency SET NOT NULL;
ALTER TABLE cart_items DROP COLUMN product_price;

-- carts
ALTER TABLE carts ADD COLUMN total_price_minor_units BIGINT;
ALTER TABLE carts ADD COLUMN currency VARCHAR(8);
UPDATE carts SET total_price_minor_units = ROUND(COALESCE(total_price, 0) * 100),
                  currency = 'INR';
ALTER TABLE carts ALTER COLUMN total_price_minor_units SET NOT NULL;
ALTER TABLE carts ALTER COLUMN currency SET NOT NULL;
ALTER TABLE carts DROP COLUMN total_price;

-- orders
ALTER TABLE orders ADD COLUMN amount_minor_units BIGINT;
ALTER TABLE orders ADD COLUMN currency VARCHAR(8);
UPDATE orders SET amount_minor_units = ROUND(COALESCE(total_amount, 0) * 100),
                   currency = 'INR';
ALTER TABLE orders ALTER COLUMN amount_minor_units SET NOT NULL;
ALTER TABLE orders ALTER COLUMN currency SET NOT NULL;
ALTER TABLE orders DROP COLUMN total_amount;

-- order_items
ALTER TABLE order_items ADD COLUMN price_minor_units BIGINT;
ALTER TABLE order_items ADD COLUMN ordered_product_price_minor_units BIGINT;
ALTER TABLE order_items ADD COLUMN currency VARCHAR(8);
UPDATE order_items SET price_minor_units = ROUND(COALESCE(price, 0) * 100),
                        ordered_product_price_minor_units = ROUND(COALESCE(ordered_product_price, 0) * 100),
                        currency = 'INR';
ALTER TABLE order_items ALTER COLUMN price_minor_units SET NOT NULL;
ALTER TABLE order_items ALTER COLUMN ordered_product_price_minor_units SET NOT NULL;
ALTER TABLE order_items ALTER COLUMN currency SET NOT NULL;
ALTER TABLE order_items DROP COLUMN price;
ALTER TABLE order_items DROP COLUMN ordered_product_price;
