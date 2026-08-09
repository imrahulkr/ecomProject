-- Demo data for showing the app on a fresh machine/database.
-- NOT a Flyway migration on purpose (lives outside src/main/resources/db/migration) - this is
-- throwaway demo content, not schema-required data, so it must never auto-run against prod.
--
-- Prerequisite: start the app once against the target DB so Hibernate (ddl-auto=update, test
-- profile) / Flyway (prod profile) create the schema and V8__seed_roles.sql seeds the roles table.
-- Then run:
--   psql "$DATABASE_URL" -f db/demo-seed.sql
-- (or: psql -U postgres -d ecommerce -f db/demo-seed.sql)
--
-- Every statement is idempotent (safe to re-run).
--
-- Demo login credentials (BCrypt-hashed below):
--   admin@demo.test    / demoadmin    / Admin@1234
--   seller@demo.test   / demoseller   / Seller@1234
--   customer@demo.test / democustomer / Customer@1234

-- ---- Users ----
INSERT INTO users (name, username, email, password, enabled, created_at, updated_at)
SELECT v.name, v.username, v.email, v.password, v.enabled, now(), now()
FROM (VALUES
    ('Demo Admin',    'demoadmin',    'admin@demo.test',    '$2b$10$DOotVD/tcmFRwDe/m/6xMu.jHnOKZKk8PUmnCchc3DGQII2T0oK1a', true),
    ('Demo Seller',   'demoseller',   'seller@demo.test',   '$2b$10$5dlF6naaa6mCj2wZib0KeOB2/kDGrVf/8OC3LgelbDU4Z3r7sOXzy', true),
    ('Demo Customer', 'democustomer', 'customer@demo.test', '$2b$10$3Cnx3e0rMbf.lr2iB.mc/uvebRDFkJk2bIa6G7JQyt0XS.gXpkyjq', true)
) AS v(name, username, email, password, enabled)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE users.username = v.username);

-- ---- Role assignments ----
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM (VALUES ('demoadmin', 'ROLE_ADMIN'), ('demoseller', 'ROLE_SELLER'), ('democustomer', 'ROLE_USER')) AS v(username, role_name)
JOIN users u ON u.username = v.username
JOIN roles r ON r.role_name = v.role_name
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id);

-- ---- Categories ----
INSERT INTO categories (category_name)
SELECT v.c
FROM (VALUES ('Electronics'), ('Fashion'), ('Home & Kitchen'), ('Books'), ('Sports & Fitness')) AS v(c)
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE category_name = v.c);

-- ---- Products (owned by the demo seller) ----
-- price_minor_units / special_price_minor_units are paise (app.currency=INR, 2 decimal places).
INSERT INTO products (product_name, description, image, quantity, price_minor_units, discount, special_price_minor_units, currency, category_id, seller_id, created_at, updated_at)
SELECT v.product_name, v.description, v.image, v.quantity, v.price_minor_units, v.discount, v.special_price_minor_units, 'INR', c.category_id, u.user_id, now(), now()
FROM (VALUES
    ('Wireless Mouse',        'Ergonomic 2.4GHz wireless mouse',        'https://picsum.photos/seed/wireless-mouse/600/600',   50,  99900, 10.0,  89910, 'Electronics'),
    ('Bluetooth Headphones',  'Over-ear noise cancelling headphones',   'https://picsum.photos/seed/bt-headphones/600/600',    30, 349900, 15.0, 297415, 'Electronics'),
    ('Men''s Cotton T-Shirt', 'Breathable everyday cotton t-shirt',     'https://picsum.photos/seed/cotton-tshirt/600/600',   100,  59900, 20.0,  47920, 'Fashion'),
    ('Non-Stick Frying Pan',  '28cm non-stick frying pan',              'https://picsum.photos/seed/frying-pan/600/600',       40, 129900,  0.0, 129900, 'Home & Kitchen'),
    ('The Pragmatic Programmer', 'Classic software craftsmanship book', 'https://picsum.photos/seed/pragmatic-prog/600/600',   25,  89900,  5.0,  85405, 'Books'),
    ('Yoga Mat',               '6mm anti-slip yoga mat',                'https://picsum.photos/seed/yoga-mat/600/600',         60,  79900, 12.5,  69913, 'Sports & Fitness')
) AS v(product_name, description, image, quantity, price_minor_units, discount, special_price_minor_units, category_name)
JOIN categories c ON c.category_name = v.category_name
JOIN users u ON u.username = 'demoseller'
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.product_name = v.product_name);

-- ---- Address for the demo customer (needed to exercise checkout) ----
INSERT INTO addresses (street, building_name, city, state, country, pincode, user_id, created_at, updated_at)
SELECT '100 MG Road', 'Demo Chambers', 'Bengaluru', 'Karnataka', 'India', '560001', u.user_id, now(), now()
FROM users u
WHERE u.username = 'democustomer'
AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.user_id = u.user_id);
