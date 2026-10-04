CREATE TABLE provider_health (
    id                    BIGSERIAL PRIMARY KEY,
    provider_name         VARCHAR(32)  NOT NULL,
    consecutive_failures  INTEGER      NOT NULL DEFAULT 0,
    cooldown_until        TIMESTAMP    NULL,
    updated_at            TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_provider_health_provider_name UNIQUE (provider_name)
);

INSERT INTO provider_health (provider_name, consecutive_failures, updated_at)
VALUES ('STRIPE', 0, now()), ('RAZORPAY', 0, now());
