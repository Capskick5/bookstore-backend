# BookVerse — Kế hoạch dự án (bản chữ)

> Bản trực quan: mở `docs/plan/index.html`. Leader theo dõi thứ tự và dependency tại `docs/plan/leader.html`. Chi tiết kỹ thuật: `docs/PROJECT-KICKOFF.md`.

## Kiến trúc (nhắc lại nhanh)
React (FE) → Spring Boot (API + PostgreSQL) → RAG service Python (Qdrant + MongoDB + OpenAI).
FE chỉ gọi Spring Boot. Spring Boot gọi RAG khi cần chatbot/gợi ý.

## Lộ trình 5 giai đoạn
1. **Giai đoạn 0 — Demo & Chốt (đang làm):** demo giao diện, chốt scope + kiến trúc.
2. **Giai đoạn 1 — Setup nền (Leader + P1):** repo, docker-compose, auth+JWT, Swagger, CI, seed.
3. **Giai đoạn 2 — Code lõi (cả nhóm):** catalog, giỏ, đơn, admin, RAG thật.
4. **Giai đoạn 3 — Tích hợp:** ráp FE↔BE↔RAG, voucher, điểm.
5. **Giai đoạn 4 — Demo cuối:** test, sửa lỗi, deploy, tập demo.

Nguyên tắc: KHÔNG sang giai đoạn sau khi giai đoạn trước chưa chạy được.

## "Cổng mở" — đủ hết mới cho cả nhóm vào code
- [ ] Cấu trúc repo: Spring Boot ở `bookstore/`, RAG ở `/rag`, hạ tầng ở `/infra`, tài liệu ở `/docs`
- [ ] docker-compose lên Postgres + Qdrant + Mongo (1 lệnh)
- [ ] Migration đầu tiên tạo bảng `users` (Flyway)
- [ ] Auth + JWT chạy: register / login / refresh / me
- [ ] Swagger bật + nút Authorize bearer
- [ ] `.env.example` đầy đủ biến
- [ ] GitHub Actions CI build + test xanh
- [ ] Seed data: 1 ADMIN + ~12 sách + category
- [ ] Phổ biến quy ước Git + Definition of Done
- [ ] Đã đẩy skeleton lên GitHub

## Phân công 6 người
| Người | Mảng | Nhiệm vụ chính |
|------|------|----------------|
| P1 | Auth & phân quyền | đăng ký/đăng nhập JWT, refresh, RBAC, profile |
| P2 | Catalog & đánh giá | danh sách+phân trang, tìm/lọc/sắp xếp, chi tiết, review |
| P3 | Giỏ & đơn hàng | giỏ, checkout trừ kho atomic, lịch sử/trạng thái, hủy |
| P4 | Admin | CRUD sách/danh mục, quản lý đơn/người dùng, voucher, dashboard |
| P5 | RAG/AI | thay OpenAI thật, reindex, gợi ý, tích hợp Spring Boot |
| Lead | DevOps & tích hợp | repo, docker, CI/CD, deploy, khung FE, review PR |

## Tuần 1 — Leader + P1 (chi tiết)
**Ngày 1:** Leader tạo monorepo + `.gitignore` + docker-compose + bê `/rag` của P5 vào + `.env.example`. P1 khởi tạo Spring Boot (Web, Security, JPA, Flyway, Swagger, validation).
**Ngày 2:** P1 làm migration users + Spring Security + JWT + register/login/refresh/me + Swagger Authorize. Leader làm `/api/health` + deploy thử Railway.
**Ngày 3:** Leader làm CI (build+test) + seed data + export Swagger (hợp đồng API). Cả nhóm pull về chạy docker-compose + login OK → **CỔNG MỞ**.

## Rủi ro & cách xử lý (xem đầy đủ trong PROJECT-KICKOFF.md §3)
Oversell kho · giá đổi · idempotency thanh toán · voucher lạm dụng · điểm lậu · chatbot gợi ý sách đã xóa · cháy ví OpenAI · IDOR · lộ key · sai số tiền.

## Backup (kỷ luật)
- Đẩy GitHub mỗi ngày ≥ 1 lần.
- Tài liệu `docs/` đi cùng repo (mất máy vẫn còn).
- File riêng `_local/` chỉ trên máy — tự sao lưu Drive/USB.
