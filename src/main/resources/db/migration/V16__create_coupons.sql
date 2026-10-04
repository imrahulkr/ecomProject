CREATE TABLE coupons (
    coupon_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL,
    discount_percentage DOUBLE PRECISION,
    discount_amount_minor_units BIGINT,
    currency VARCHAR(8),
    min_order_amount_minor_units BIGINT,
    max_redemptions INTEGER,
    per_user_limit INTEGER,
    expires_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE coupon_redemptions (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL REFERENCES coupons(coupon_id),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    order_id BIGINT NOT NULL REFERENCES orders(order_id),
    discount_minor_units BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_coupon_redemptions_coupon_id ON coupon_redemptions(coupon_id);
CREATE INDEX idx_coupon_redemptions_coupon_user ON coupon_redemptions(coupon_id, user_id);

-- Cart's currently-applied coupon, if any - see Cart.java/CouponServiceImpl for how this is kept
-- in sync (revalidated against live coupon state at checkout, not trusted as-is).
ALTER TABLE carts ADD COLUMN applied_coupon_code VARCHAR(64);
ALTER TABLE carts ADD COLUMN discount_minor_units BIGINT NOT NULL DEFAULT 0;

-- Snapshot of the coupon (if any) redeemed for this order, kept even if the coupon is later
-- deleted/deactivated so order history stays accurate.
ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(64);
ALTER TABLE orders ADD COLUMN discount_minor_units BIGINT NOT NULL DEFAULT 0;
