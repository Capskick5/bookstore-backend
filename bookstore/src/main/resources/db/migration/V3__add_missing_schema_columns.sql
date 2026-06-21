ALTER TABLE users ADD COLUMN lifetime_points BIGINT NOT NULL DEFAULT 0 CHECK (lifetime_points >= 0);

ALTER TABLE vouchers ADD COLUMN used_count INT NOT NULL DEFAULT 0 CHECK (used_count >= 0);

ALTER TABLE orders ADD COLUMN payment_transaction_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN expires_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE voucher_redemptions ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACTIVE'));

ALTER TABLE conversations ADD COLUMN title VARCHAR(255);
