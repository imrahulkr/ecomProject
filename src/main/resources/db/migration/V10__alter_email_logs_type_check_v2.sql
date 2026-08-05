-- Same reason as V7: Hibernate's auto-generated CHECK constraint on email_logs.email_type
-- doesn't get widened by ddl-auto=update when EmailType gains new values.
ALTER TABLE email_logs DROP CONSTRAINT IF EXISTS email_logs_email_type_check;

ALTER TABLE email_logs ADD CONSTRAINT email_logs_email_type_check
    CHECK (email_type IN (
        'VERIFICATION', 'PASSWORD_RESET', 'ORDER_CONFIRMATION', 'SHIPPING_UPDATE',
        'DELIVERY_CONFIRMATION', 'ABANDONED_CART', 'PROMOTIONAL', 'REFUND_PROCESSED',
        'PAYMENT_FAILED', 'SELLER_APPLICATION_APPROVED', 'SELLER_APPLICATION_REJECTED'
    ));
