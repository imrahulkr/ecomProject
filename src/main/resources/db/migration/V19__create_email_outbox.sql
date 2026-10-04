-- Transactional outbox for email. A row is written in the same transaction as the change that
-- triggers the email (order paid, password changed, ...), so the email exists only if that change
-- committed, and survives restarts. EmailOutboxSender delivers due rows outside any transaction.
CREATE TABLE email_outbox (
    id              BIGSERIAL PRIMARY KEY,
    recipient       VARCHAR(320) NOT NULL,
    sender          VARCHAR(320),
    subject         VARCHAR(998) NOT NULL,
    html_body       TEXT,
    text_body       TEXT,
    email_type      VARCHAR(64),
    metadata_json   TEXT,
    status          VARCHAR(16)  NOT NULL,
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NOT NULL,
    last_error      TEXT,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL
);

CREATE INDEX idx_email_outbox_due ON email_outbox (status, next_attempt_at);
