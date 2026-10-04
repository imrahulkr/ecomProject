CREATE TABLE processed_webhook_event (
    id                 BIGSERIAL PRIMARY KEY,
    provider_name      VARCHAR(32)  NOT NULL,
    provider_event_id  VARCHAR(255) NOT NULL,
    processed_at       TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_processed_webhook_event_provider_event UNIQUE (provider_name, provider_event_id)
);
