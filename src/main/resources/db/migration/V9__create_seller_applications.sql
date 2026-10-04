CREATE TABLE seller_applications (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT       NOT NULL REFERENCES users (user_id),
    business_name          VARCHAR(255) NOT NULL,
    business_description   TEXT,
    status                 VARCHAR(16)  NOT NULL,
    rejection_reason       TEXT,
    applied_at             TIMESTAMP    NOT NULL,
    decided_at             TIMESTAMP,
    decided_by_admin_id    BIGINT
);

CREATE INDEX idx_seller_applications_user_id ON seller_applications (user_id);
CREATE INDEX idx_seller_applications_status ON seller_applications (status);
