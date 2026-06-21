-- Seed categories (idempotent)
INSERT INTO categories (name, slug) VALUES
    ('Fiction', 'fiction'),
    ('Technology', 'technology'),
    ('Business', 'business')
ON CONFLICT (slug) DO NOTHING;

-- Seed books (only if slug category exists and title not duplicated via id)
INSERT INTO books (title, author, category_id, price, original_price, stock, description, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Clean Code', 'Robert Martin', c.id, 350000, 400000, 50, 'Software craftsmanship', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'technology'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Clean Code');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'The Pragmatic Programmer', 'Hunt & Thomas', c.id, 320000, 380000, 30, 'Pragmatic advice for developers', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'technology'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'The Pragmatic Programmer');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Atomic Habits', 'James Clear', c.id, 280000, 320000, 100, 'Build good habits', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'business'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Atomic Habits');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Dune', 'Frank Herbert', c.id, 250000, 300000, 20, 'Sci-fi classic', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'fiction'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Dune');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Inactive Book', 'Nobody', c.id, 100000, 100000, 5, 'Inactive for guest merge tests', 0, 0, false, now(), now()
FROM categories c WHERE c.slug = 'fiction'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Inactive Book');

-- Seed vouchers
INSERT INTO vouchers (code, type, value, min_order, max_discount, usage_limit, per_user_limit, starts_at, ends_at, active, used_count)
VALUES
    ('SAVE50K', 'FIXED', 50000, 200000, NULL, 100, 5, now() - interval '1 day', now() + interval '365 days', true, 0),
    ('PERCENT10', 'PERCENT', 10, 100000, 80000, 50, 3, now() - interval '1 day', now() + interval '365 days', true, 0),
    ('FREESHIP', 'SHIP', 30000, 0, NULL, NULL, 10, now() - interval '1 day', now() + interval '365 days', true, 0)
ON CONFLICT (code) DO NOTHING;
