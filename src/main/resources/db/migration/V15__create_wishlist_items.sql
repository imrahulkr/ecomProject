CREATE TABLE wishlist_items (
    wishlist_item_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_wishlist_items_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_wishlist_items_user_id ON wishlist_items(user_id);
