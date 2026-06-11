-- ============================================================
-- BookVerse — PostgreSQL schema (tham khảo / khởi tạo Flyway V1)
-- Tiền tệ: lưu bằng BIGINT VND (KHÔNG dùng float/decimal cho tiền).
-- ============================================================

-- ---------- Người dùng & địa chỉ ----------
CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name     VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER'
                           CHECK (role IN ('ADMIN','CUSTOMER')),
                       enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
                       points        BIGINT       NOT NULL DEFAULT 0,
                       tier          VARCHAR(20)  NOT NULL DEFAULT 'SILVER'
                           CHECK (tier IN ('SILVER','GOLD','PLATINUM')),
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE addresses (
                           id         BIGSERIAL PRIMARY KEY,
                           user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           recipient  VARCHAR(255) NOT NULL,
                           phone      VARCHAR(20)  NOT NULL,
                           line       VARCHAR(255) NOT NULL,
                           city       VARCHAR(100) NOT NULL,
                           is_default BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_addresses_user ON addresses(user_id);

-- ---------- Danh mục & sách ----------
CREATE TABLE categories (
                            id   BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) UNIQUE NOT NULL,
                            slug VARCHAR(120) UNIQUE NOT NULL
);

CREATE TABLE books (
                       id             BIGSERIAL PRIMARY KEY,
                       title          VARCHAR(255) NOT NULL,
                       author         VARCHAR(255) NOT NULL,
                       category_id    BIGINT REFERENCES categories(id),
                       price          BIGINT NOT NULL CHECK (price >= 0),       -- VND
                       original_price BIGINT CHECK (original_price >= 0),       -- VND
                       stock          INT    NOT NULL DEFAULT 0 CHECK (stock >= 0),
                       description    TEXT,
                       cover_url      VARCHAR(500),
                       rating_avg     NUMERIC(2,1) NOT NULL DEFAULT 0,
                       sold_count     INT NOT NULL DEFAULT 0,
                       active         BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_books_category ON books(category_id);
CREATE INDEX idx_books_price    ON books(price);
CREATE INDEX idx_books_title    ON books(LOWER(title));
CREATE INDEX idx_books_author   ON books(LOWER(author));

-- ---------- Đánh giá ----------
CREATE TABLE reviews (
                         id         BIGSERIAL PRIMARY KEY,
                         book_id    BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
                         user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         rating     INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment    TEXT,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         UNIQUE (book_id, user_id)   -- mỗi user chỉ 1 review / sách
);
CREATE INDEX idx_reviews_book ON reviews(book_id);

-- ---------- Giỏ hàng ----------
CREATE TABLE carts (
                       id      BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
                            id       BIGSERIAL PRIMARY KEY,
                            cart_id  BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
                            book_id  BIGINT NOT NULL REFERENCES books(id),
                            quantity INT NOT NULL CHECK (quantity > 0),
                            UNIQUE (cart_id, book_id)
);

-- ---------- Voucher ----------
CREATE TABLE vouchers (
                          id             BIGSERIAL PRIMARY KEY,
                          code           VARCHAR(50) UNIQUE NOT NULL,
                          type           VARCHAR(10) NOT NULL CHECK (type IN ('FIXED','PERCENT','SHIP')),
                          value          BIGINT NOT NULL,            -- FIXED: VND, PERCENT: %, SHIP: số tiền ship giảm
                          min_order      BIGINT NOT NULL DEFAULT 0,  -- VND
                          max_discount   BIGINT,                     -- trần giảm cho PERCENT
                          usage_limit    INT,                        -- tổng lượt dùng (NULL = không giới hạn)
                          per_user_limit INT NOT NULL DEFAULT 1,
                          starts_at      TIMESTAMPTZ,
                          ends_at        TIMESTAMPTZ,
                          active         BOOLEAN NOT NULL DEFAULT TRUE
);

-- ---------- Đơn hàng ----------
CREATE TABLE orders (
                        id               BIGSERIAL PRIMARY KEY,
                        user_id          BIGINT NOT NULL REFERENCES users(id),
                        status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','PAID','SHIPPED','DELIVERED','CANCELLED')),
                        subtotal         BIGINT NOT NULL,   -- VND
                        discount         BIGINT NOT NULL DEFAULT 0,
                        shipping_fee     BIGINT NOT NULL DEFAULT 0,
                        total            BIGINT NOT NULL,
                        address_snapshot TEXT NOT NULL,
                        payment_method   VARCHAR(20) NOT NULL,
                        voucher_code     VARCHAR(50),
                        points_used      BIGINT NOT NULL DEFAULT 0,
                        points_earned    BIGINT NOT NULL DEFAULT 0,
                        idempotency_key  VARCHAR(100) UNIQUE,   -- chống đặt trùng khi bấm nhiều lần
                        created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

CREATE TABLE order_items (
                             id             BIGSERIAL PRIMARY KEY,
                             order_id       BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             book_id        BIGINT NOT NULL REFERENCES books(id),
                             title_snapshot VARCHAR(255) NOT NULL,   -- lưu tên sách lúc mua
                             unit_price     BIGINT NOT NULL,         -- lưu giá lúc mua (price snapshot)
                             quantity       INT NOT NULL CHECK (quantity > 0)
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

-- ---------- Lượt dùng voucher ----------
CREATE TABLE voucher_redemptions (
                                     id         BIGSERIAL PRIMARY KEY,
                                     voucher_id BIGINT NOT NULL REFERENCES vouchers(id),
                                     user_id    BIGINT NOT NULL REFERENCES users(id),
                                     order_id   BIGINT NOT NULL REFERENCES orders(id),
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     UNIQUE (voucher_id, order_id)
);
CREATE INDEX idx_redemptions_voucher_user ON voucher_redemptions(voucher_id, user_id);

-- ---------- Giao dịch điểm thưởng ----------
CREATE TABLE point_transactions (
                                    id         BIGSERIAL PRIMARY KEY,
                                    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    order_id   BIGINT REFERENCES orders(id),
                                    delta      BIGINT NOT NULL,   -- + khi tích, - khi đổi/hoàn
                                    reason     VARCHAR(50) NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_points_user ON point_transactions(user_id);

-- ---------- Hội thoại chatbot ----------
CREATE TABLE conversations (
                               id         BIGSERIAL PRIMARY KEY,
                               user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE messages (
                          id              BIGSERIAL PRIMARY KEY,
                          conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                          role            VARCHAR(10) NOT NULL CHECK (role IN ('user','assistant')),
                          content         TEXT NOT NULL,
                          sources_json    JSONB,   -- nguồn RAG trả về (dẫn chứng)
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages(conversation_id);

-- ============================================================
-- LƯU Ý: Qdrant (vector) + MongoDB (chunk/ảnh) thuộc service RAG (Python),
-- KHÔNG nằm trong schema quan hệ này.
-- ============================================================
