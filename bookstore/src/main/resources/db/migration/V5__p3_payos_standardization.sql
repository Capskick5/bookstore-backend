ALTER TABLE orders ADD COLUMN IF NOT EXISTS manual_refund_required BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    ALTER TABLE users ADD CONSTRAINT users_points_non_negative CHECK (points >= 0);
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
