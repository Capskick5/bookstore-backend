# BookVerse — Kickoff & Sổ tay kỹ thuật (cho cả nhóm)

> Tài liệu nguồn chân lý cho dự án. Đọc trước khi code. Cập nhật khi có thay đổi.

## 1. Kiến trúc tổng thể

```
React (Vercel)
   │ REST + JWT
   ▼
Spring Boot  ── PostgreSQL (users, books, orders, vouchers, points...)
   │ HTTP nội bộ (gọi khi chatbot/gợi ý)
   ▼
Python RAG service (FastAPI)  ── Qdrant (vector) + MongoDB (chunks/ảnh) + OpenAI
```

- **Spring Boot** là API chính: auth, catalog, cart, order, admin, voucher, điểm. DB quan hệ = PostgreSQL.
- **RAG service (Python, của P5)** là microservice riêng: ingest sách (PDF/EPUB) → Qdrant+MongoDB, trả lời truy vấn. Spring Boot gọi nó qua HTTP, KHÔNG truy cập thẳng Qdrant/Mongo.
- **Quy ước biên giới:** mọi thứ "AI" đi qua RAG service. Mọi thứ "nghiệp vụ" đi qua Spring Boot. FE chỉ nói chuyện với Spring Boot (Spring Boot proxy sang RAG khi cần).

## 2. Quyết định đã chốt (Architecture Decisions)

| # | Vấn đề | Quyết định |
|---|--------|-----------|
| 1 | Vector store | Qdrant |
| 2 | LLM/Embedding | OpenAI (gpt-4o-mini + text-embedding-3-small, dim 1536) |
| 3 | RAG | Microservice Python (FastAPI) riêng, của P5; thay FakeOpenAIService bằng OpenAI thật |
| 4 | MongoDB | Chỉ dùng NỘI BỘ trong RAG service (lưu chunk/ảnh). Nghiệp vụ vẫn PostgreSQL |
| 5 | Tiền tệ | Lưu bằng số nguyên VND (long), KHÔNG dùng float |
| 6 | Migration | Flyway (versioned), không auto-DDL |
| 7 | Chạy local | docker-compose: postgres + qdrant + mongo (+ services) |
| 8 | Git | 1 nhánh `main`, commit nhỏ thường xuyên, pull --rebase trước khi push |

## 3. Giải pháp cho các "câu hỏi chết người"

| Rủi ro | Hướng xử lý (bắt buộc) |
|--------|------------------------|
| Bán vượt kho (oversell) khi 2 người mua cùng lúc | Trừ kho trong transaction + `UPDATE books SET stock=stock-:q WHERE id=:id AND stock>=:q` atomically; nếu 0 dòng bị ảnh hưởng → báo hết hàng. |
| Giá đổi giữa lúc thêm giỏ và thanh toán | Giỏ hàng dùng giá real-time (JOIN bảng `books`). Checkout xác nhận lại giá hiện tại, chụp lại `unit_price` snapshot vào `order_items` lúc đặt đơn. |
| Thanh toán OK nhưng tạo đơn lỗi | Idempotency key cho checkout; tạo đơn trạng thái `PENDING` trước, sau đó IPN webhook callback cập nhật trạng thái `PAID` / `CANCELLED`. |
| Bấm Đặt hàng nhiều lần | Idempotency key + disable nút; chống tạo đơn trùng (kiểm tra key trong DB trước khi INSERT). |
| Voucher bị lạm dụng | Bảng `voucher_redemptions`: giới hạn lần dùng tổng + mỗi user; check hạn + đơn tối thiểu; KHÔNG cộng dồn voucher. Tăng lượt dùng atomically: `UPDATE vouchers SET used_count = used_count + 1 WHERE id = :id AND (usage_limit IS NULL OR used_count < usage_limit) AND active = true`. Discount tối đa không vượt quá subtotal. |
| Điểm "lậu" khi hủy đơn | Cộng điểm khi đơn = `DELIVERED` (không phải lúc đặt); hủy/hoàn → ghi `point_transactions` âm để thu hồi. Điểm cập nhật bằng delta SQL update hoặc Pessimistic Locking. Database-level CHECK constraint ngăn points < 0. |
| Chatbot gợi ý sách đã xóa/sai giá | Khi admin sửa/xóa/ẩn sách → Spring Boot bắn `BookChangedEvent` bất đồng bộ để RAG service reindex. Gợi ý sách phải lọc bỏ các sách `active = false` hoặc `stock = 0`. Tạo payload index trên `book_id` trong Qdrant để tối ưu hóa việc xóa vector. Chạy scheduled reconciliation cron job hàng đêm để xử lý các event bị lỗi hoặc vector bị đồng bộ thiếu. |
| Chatbot bịa (hallucinate) | Prompt ràng buộc "chỉ dùng sources", phân tách rõ ràng System Prompt và User Message để chống Prompt Injection; lọc từ khóa injection độc hại; nếu không có source đạt ngưỡng → trả lời "không có trong dữ liệu". |
| Cháy ví OpenAI | Rate-limit 20 request/phút theo user; giới hạn top_k (default 5, max 20) và độ dài context tối đa 10 tin nhắn gần nhất (độ dài context là 10 tin nhắn chronologically gần nhất của user và assistant, loại trừ system prompt). |
| Prompt injection | Tách system prompt; không cho input người dùng ghi đè chỉ dẫn; lọc nội dung. |
| IDOR (xem đơn người khác) | Mọi endpoint theo tài nguyên cá nhân phải kiểm tra `resource.userId == currentUser.id`, sai → 403. ADMIN được quyền xóa review độc hại. |
| Token sau đăng xuất vẫn sống | Access token ngắn (15') + refresh rotation; phát hiện replay refresh token cũ lập tức thu hồi cả family; đổi mật khẩu hoặc bị disable tài khoản sẽ revoke toàn bộ refresh tokens và check trạng thái `enabled` mỗi request. |
| Tạo ADMIN trái phép | Đăng ký công khai luôn ra CUSTOMER; chỉ ADMIN mới đổi role; seed sẵn 1 ADMIN. |
| Ảnh bìa | Lưu URL trỏ object storage/CDN, KHÔNG nhồi binary vào DB quan hệ. |
| Catalog chậm | Phân trang server-side (default 10, max 50) + index DB (title, category_id, price). Hỗ trợ sort option cụ thể. |
| Lộ secret | `.env` trong `.gitignore`; key đặt ở GitHub Secrets / biến môi trường; KHÔNG commit. CORS allowed origins đọc từ env `APP_CORS_ALLOWED_ORIGINS`. |
| Hạng thành viên (Tier upgrade) | Nâng hạng tự động dựa trên tổng điểm tích lũy trọn đời (`users.lifetime_points`: SILVER < 1000, GOLD 1000-4999, PLATINUM >= 5000). Điểm tích lũy khi đơn hàng thành công sẽ cộng vào cả points và lifetime_points. Khi tiêu dùng điểm, lifetime_points giữ nguyên. Khi đơn hàng bị hủy/hoàn, điểm bị thu hồi sẽ trừ ở cả points và lifetime_points (giới hạn tối thiểu là 0). Trong Phase 1, tier = cosmetic only. |
| Tích hợp Thanh toán generic | Khi checkout, Spring Boot tạo đơn ở trạng thái `PENDING`, giữ kho tạm trong 15 phút, tăng `vouchers.used_count` atomically, và trả về gateway Payment URL (VNPAY/PayOS) từ `PaymentServiceFactory`. URL thanh toán VNPAY cần truyền `vnp_ExpireDate` khớp với 15 phút giữ kho. Webhook callback nhận ở `/api/payment/webhook/{provider}` dùng hash key bí mật check signature, endpoint này được whitelist trong SecurityConfig (permitAll/CORS). Để tránh race condition giữa webhook và người dùng hủy đơn / scheduler timeout, dùng Pessimistic Locking (SELECT FOR UPDATE) lên dòng order tương ứng trước khi xử lý. Nếu thanh toán thành công → chuyển đơn sang `PAID`, tăng `sold_count` atomically, chốt voucher, xóa giỏ hàng. Nếu thanh toán thất bại hoặc timeout 15 phút → chuyển sang `CANCELLED`, restore kho atomically, xóa `voucher_redemptions` và giảm `used_count` voucher atomically. |
| Quy đổi điểm thưởng | Cộng điểm: 10.000 VND spent (tính trên total đã thanh toán) = 1 điểm. Tiêu điểm: 1 điểm = 100 VND giảm trừ trực tiếp. Tiêu điểm tối đa 20% tổng giá trị đơn hàng. KHÔNG kết hợp voucher và điểm cùng lúc. Công thức: `Discounted Subtotal = Max(0, Subtotal - Discount_Amount)`. |
| Gộp giỏ hàng (Cart Merge) | Khi người dùng đăng nhập, tự động gộp giỏ hàng tạm (Guest) vào giỏ hàng DB (nếu trùng sách thì cộng dồn số lượng tối đa theo tồn kho của sách). Bỏ qua các sách có `active == false`. Trong quá trình gộp, cập nhật giá của giỏ hàng theo giá catalog hiện tại của sách và bỏ qua giá cũ từ giỏ guest. |
| Sách ngừng hoạt động | Sách có `active = false` hoặc sách bị xóa mà có orders (soft-delete) sẽ bị ẩn khỏi Catalog công khai, search và gợi ý AI. Chặn checkout các sách inactive. Khi soft-delete, tự động xóa sách khỏi mọi `cart_items` để tránh FK violation. |
| Shipping fee | Phí ship cố định là 30.000 VND. Miễn phí ship (0 VND) khi đơn hàng có net total (subtotal - discount) >= 300.000 VND hoặc có voucher loại `SHIP`. Công thức: `Net Total = Discounted Subtotal + Shipping Fee`. |
| RAG reindex async | Admin trigger reindex trả về 202 Accepted ngay, chạy ngầm với retry exponential backoff 3 lần. Dashboard hiển thị trạng thái và "Last Indexed". |
| EPUB/PDF parsing complexity | Chỉ trích xuất text đơn thuần (text-only fallback) dùng thư viện chuẩn (`pypdf`/`pymupdf` và `ebooklib`), loại bỏ hoàn toàn các hình ảnh phức tạp, công thức toán học và layout cầu kỳ để tránh bẫy tốn thời gian (time traps). |
| Lỗi Security 401/403 lệch chuẩn | Đăng ký custom `AuthenticationEntryPoint` và `AccessDeniedHandler` trong Spring Security để tự ghi trực tiếp vào output stream của response định dạng JSON chuẩn chung `{ timestamp, status, error, message, path }`, không trả về body trống mặc định của Spring Boot. |
| Tràn bảng refresh tokens | Chạy scheduled cron job hàng ngày quét và xóa toàn bộ các refresh token đã hết hạn trong DB (`refresh_tokens` table) để giải phóng bộ nhớ. |

## 4. ERD (PostgreSQL — Spring Boot)

```
users(id, email[uniq], password_hash, full_name, role[ADMIN|CUSTOMER],
      enabled, points[>=0], lifetime_points[>=0], tier, created_at)
refresh_tokens(id, user_id->users, token_hash[uniq], family_id, used, expires_at, created_at)
addresses(id, user_id->users, recipient, phone, line, city, is_default)
categories(id, name[uniq], slug)
books(id, title, author, category_id->categories, price, original_price,
      stock, description, cover_url, rating_avg, sold_count, active,
      created_at, updated_at)
reviews(id, book_id->books, user_id->users, rating[1..5], comment, created_at)
      -- UNIQUE(book_id, user_id)
carts(id, user_id->users[uniq])
cart_items(id, cart_id->carts, book_id->books, quantity)
orders(id, user_id->users, status[PENDING|PAID|SHIPPED|DELIVERED|CANCELLED],
       subtotal, discount, shipping_fee, total, address_snapshot,
       payment_method, payment_transaction_id, voucher_code, points_used, points_earned,
       expires_at, created_at, updated_at)
order_items(id, order_id->orders, book_id->books, title_snapshot,
            unit_price, quantity)
vouchers(id, code[uniq], type[FIXED|PERCENT|SHIP], value, min_order,
         max_discount, usage_limit, used_count, per_user_limit, starts_at, ends_at, active)
voucher_redemptions(id, voucher_id->vouchers, user_id->users, order_id->orders,
                    status[PENDING|ACTIVE], created_at)
      -- UNIQUE(voucher_id, order_id)
point_transactions(id, user_id->users, order_id->orders, delta, reason, created_at)
conversations(id, user_id->users, title, created_at)
messages(id, conversation_id->conversations, role[user|assistant], content,
         sources_json, created_at)
```
RAG service (nội bộ, P5 sở hữu): MongoDB `books_rag.{books,chunks,images}` + Qdrant collection `books`.

## 5. API contract (rút gọn — đầy đủ trong Swagger)

Auth: `POST /auth/register` · `POST /auth/login` · `POST /auth/refresh` · `GET /auth/me`
Catalog: `GET /books?page&size&q&category&minPrice&maxPrice&sort` · `GET /books/{id}` · `GET /categories`
Reviews: `GET /books/{id}/reviews` · `POST /books/{id}/reviews` · `DELETE /reviews/{id}`
Cart: `GET /cart` · `POST /cart/items` · `PUT /cart/items/{id}` · `DELETE /cart/items/{id}`
Order: `POST /orders/checkout` (Idempotency-Key) · `GET /orders` · `GET /orders/{id}` · `POST /orders/{id}/cancel`
Voucher: `POST /vouchers/apply` · `GET /me/vouchers`
Account: `GET /me/points` · `GET /me` · `PUT /me`
AI (Spring Boot proxy → RAG): `POST /ai/chat {message, conversationId?}` · `GET /ai/conversations` · `DELETE /ai/conversations/{id}`
Admin: `POST|PUT|DELETE /admin/books` · `/admin/categories` · `GET|PUT /admin/orders` · `GET|PUT /admin/users` · `/admin/vouchers` · `POST /admin/ai/reindex` · `GET /admin/stats`
RAG service (P5, nội bộ): `GET /health` · `POST /ingest` · `POST /query {query, top_k}`

Quy ước lỗi (toàn hệ thống): JSON `{ timestamp, status, error, message, path }`.

## 6. Screen flow

Khách: Trang chủ → Cửa hàng → Chi tiết → Giỏ → (Đăng nhập nếu cần) → Thanh toán → Đơn hàng/Điểm.
AI chat: mở được ở mọi màn. Wishlist/Tài khoản: cần đăng nhập.
Admin: Đăng nhập(ADMIN) → Dashboard → Sách/Danh mục/Voucher/Đơn/Người dùng → sửa sách thì trigger reindex.

## 7. Quy ước Git (thầy yêu cầu 1 nhánh)

- Chỉ 1 nhánh `main`. Commit nhỏ, message rõ: `feat(auth): login endpoint`, `fix(cart): subtotal`.
- LUÔN `git pull --rebase origin main` trước khi code và trước khi push.
- Khi conflict: bình tĩnh mở file, chọn/gộp, chạy lại test, commit, push.
- Chủ sở hữu file dùng chung (cấu hình, routing, pom.xml/package.json): hẹn nhau trước khi sửa.

## 8. Definition of Done (mỗi tính năng)

- [ ] Có endpoint + validate input + trả lỗi đúng JSON schema
- [ ] Phân quyền đúng (test cả trường hợp 401/403)
- [ ] Có test (unit/integration) cho luồng chính + 1 edge case
- [ ] Hiện trong Swagger
- [ ] FE gọi được thật (không mock)
- [ ] Chạy được bằng docker-compose trên máy người khác
