-- Hibernate auto-generates a CHECK constraint from the EmailType enum's values on first DDL
-- (email_logs is otherwise Hibernate-managed, not Flyway-owned) - ddl-auto=update doesn't alter
-- existing constraints when the enum gains a value, so PAYMENT_FAILED has to be added by hand.
ALTER TABLE email_logs DROP CONSTRAINT IF EXISTS email_logs_email_type_check;

ALTER TABLE email_logs ADD CONSTRAINT email_logs_email_type_check
    CHECK (email_type IN (
        'VERIFICATION', 'PASSWORD_RESET', 'ORDER_CONFIRMATION', 'SHIPPING_UPDATE',
        'DELIVERY_CONFIRMATION', 'ABANDONED_CART', 'PROMOTIONAL', 'REFUND_PROCESSED',
        'PAYMENT_FAILED'
    ));
