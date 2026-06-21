-- Extra demo catalog for manual/E2E testing with low VND prices.
-- Does not modify existing V4 seed books (CartOrderTests rely on their prices).

INSERT INTO categories (name, slug) VALUES
    ('Self-Help', 'self-help'),
    ('Children', 'children'),
    ('Demo Budget', 'demo-budget')
ON CONFLICT (slug) DO NOTHING;

-- Cheap fiction / tech / business titles
INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Demo Novel Alpha', 'Demo Author', c.id, 45000, 65000, 120,
       'Sach demo gia re de test gio hang va checkout COD.',
       'https://via.placeholder.com/200x300?text=Demo+Alpha', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'fiction'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Demo Novel Alpha');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Demo Novel Beta', 'Demo Author', c.id, 55000, 75000, 80,
       'Sach demo gia re, phu hop test voucher nho.',
       'https://via.placeholder.com/200x300?text=Demo+Beta', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'fiction'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Demo Novel Beta');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Quick Read Pamphlet', 'Demo Author', c.id, 25000, 35000, 200,
       'Don gia thap nhat de test nhanh checkout + phi ship.',
       'https://via.placeholder.com/200x300?text=Quick+Read', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'demo-budget'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Quick Read Pamphlet');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Budget Bundle Book', 'Demo Author', c.id, 99000, 120000, 60,
       'Gan nguong mien phi ship 300k khi mua nhieu cuon.',
       'https://via.placeholder.com/200x300?text=Budget+99k', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'demo-budget'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Budget Bundle Book');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'HTML CSS Basics', 'Web Team', c.id, 35000, 50000, 90,
       'Sach ky thuat gia re cho test catalog.',
       'https://via.placeholder.com/200x300?text=HTML+CSS', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'technology'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'HTML CSS Basics');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'JavaScript 101', 'Web Team', c.id, 49000, 69000, 75,
       'Sach lap trinh gia re.',
       'https://via.placeholder.com/200x300?text=JS+101', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'technology'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'JavaScript 101');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Git for Beginners', 'Dev Team', c.id, 39000, 55000, 85,
       'Sach cong nghe gia re.',
       'https://via.placeholder.com/200x300?text=Git+101', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'technology'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Git for Beginners');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Startup 101', 'Biz Coach', c.id, 59000, 79000, 70,
       'Sach kinh doanh gia re.',
       'https://via.placeholder.com/200x300?text=Startup', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'business'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Startup 101');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Time Management Tips', 'Biz Coach', c.id, 32000, 45000, 95,
       'Sach kinh doanh gia re de test gio nhieu san pham.',
       'https://via.placeholder.com/200x300?text=Time+Mgmt', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'business'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Time Management Tips');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Motivation Daily', 'Coach Linh', c.id, 28000, 40000, 110,
       'Sach self-help gia re.',
       'https://via.placeholder.com/200x300?text=Motivation', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'self-help'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Motivation Daily');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Mindful Living', 'Coach Linh', c.id, 42000, 60000, 100,
       'Sach self-help gia re.',
       'https://via.placeholder.com/200x300?text=Mindful', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'self-help'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Mindful Living');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Alphabet Adventure', 'Kids Press', c.id, 22000, 30000, 150,
       'Sach thieu nhi gia re.',
       'https://via.placeholder.com/200x300?text=ABC+Book', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'children'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Alphabet Adventure');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Numbers Fun', 'Kids Press', c.id, 24000, 32000, 140,
       'Sach thieu nhi gia re.',
       'https://via.placeholder.com/200x300?text=123+Book', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'children'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Numbers Fun');

INSERT INTO books (title, author, category_id, price, original_price, stock, description, cover_url, rating_avg, sold_count, active, created_at, updated_at)
SELECT 'Combo Test Pack', 'Demo Author', c.id, 150000, 180000, 40,
       'Gia vua du de test mua 2 cuon gan mien phi ship.',
       'https://via.placeholder.com/200x300?text=Combo+150k', 0, 0, true, now(), now()
FROM categories c WHERE c.slug = 'demo-budget'
  AND NOT EXISTS (SELECT 1 FROM books b WHERE b.title = 'Combo Test Pack');

-- Vouchers with low minimum order for cheap-book testing
INSERT INTO vouchers (code, type, value, min_order, max_discount, usage_limit, per_user_limit, starts_at, ends_at, active, used_count)
VALUES
    ('SAVE10K', 'FIXED', 10000, 30000, NULL, 200, 10, now() - interval '1 day', now() + interval '365 days', true, 0),
    ('MINI5K', 'FIXED', 5000, 20000, NULL, 200, 10, now() - interval '1 day', now() + interval '365 days', true, 0),
    ('TEST20', 'PERCENT', 20, 25000, 15000, 100, 5, now() - interval '1 day', now() + interval '365 days', true, 0),
    ('CHEAPSHIP', 'SHIP', 30000, 0, NULL, NULL, 20, now() - interval '1 day', now() + interval '365 days', true, 0)
ON CONFLICT (code) DO NOTHING;

-- Cover URLs for legacy V4 books that were seeded without images
UPDATE books SET cover_url = 'https://via.placeholder.com/200x300?text=Clean+Code'
WHERE title = 'Clean Code' AND cover_url IS NULL;

UPDATE books SET cover_url = 'https://via.placeholder.com/200x300?text=Pragmatic'
WHERE title = 'The Pragmatic Programmer' AND cover_url IS NULL;

UPDATE books SET cover_url = 'https://via.placeholder.com/200x300?text=Atomic+Habits'
WHERE title = 'Atomic Habits' AND cover_url IS NULL;

UPDATE books SET cover_url = 'https://via.placeholder.com/200x300?text=Dune'
WHERE title = 'Dune' AND cover_url IS NULL;
