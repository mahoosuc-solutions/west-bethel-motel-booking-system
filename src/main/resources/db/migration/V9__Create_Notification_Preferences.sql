-- Create notification_preferences table
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,

    -- Email preferences
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    booking_confirmations BOOLEAN NOT NULL DEFAULT TRUE,
    payment_receipts BOOLEAN NOT NULL DEFAULT TRUE,
    loyalty_updates BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_emails BOOLEAN NOT NULL DEFAULT FALSE,
    security_alerts BOOLEAN NOT NULL DEFAULT TRUE,

    -- Future: SMS preferences
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit fields
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_notification_preferences_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

-- Create a trigger to automatically update updated_at
CREATE OR REPLACE FUNCTION update_notification_preferences_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_notification_preferences_timestamp
    BEFORE UPDATE ON notification_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_notification_preferences_timestamp();

-- Add comment to table
COMMENT ON TABLE notification_preferences IS 'Stores user notification preferences for email and SMS communications';
COMMENT ON COLUMN notification_preferences.email_enabled IS 'Master switch for all email notifications';
COMMENT ON COLUMN notification_preferences.booking_confirmations IS 'Receive booking confirmation emails';
COMMENT ON COLUMN notification_preferences.payment_receipts IS 'Receive payment receipt emails';
COMMENT ON COLUMN notification_preferences.loyalty_updates IS 'Receive loyalty points update emails';
COMMENT ON COLUMN notification_preferences.promotional_emails IS 'Receive promotional and marketing emails';
COMMENT ON COLUMN notification_preferences.security_alerts IS 'Receive security alert emails (cannot be disabled)';
COMMENT ON COLUMN notification_preferences.sms_enabled IS 'Master switch for all SMS notifications (future feature)';
