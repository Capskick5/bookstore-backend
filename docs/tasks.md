# Implementation Plan

## Overview

This plan covers the **backend + AI** repository only: the Spring Boot REST API (PostgreSQL via Flyway) and the Python RAG microservice in `/rag`. The React frontend lives in a separate repository and is out of scope here. Money is stored as integer VND throughout. Tasks are organized so they map to the six workstreams (WS1 Auth, WS2 Catalog/Reviews, WS3 Cart/Orders, WS4 Admin, WS5 AI/RAG, WS6 Platform/DevOps), enabling six developers to work in parallel after the Phase 1 "setup gate" is complete.

## Tasks

- [ ] 1. Initialize repository structure and tooling
  - Create layout: Spring Boot at root (`src/`, `pom.xml`), `rag/` for the Python service, `infra/` for compose, `docs/` present
  - Ensure `.gitignore` covers `.env`, `target/`, `__pycache__/`, `data/`, `assets/books/`, `_local/`
  - Add `.env.example` listing all variables (DB, JWT secret, OPENAI_API_KEY, QDRANT_URL, MONGO_URL, RAG_SERVICE_URL)
  - _Requirements: 26.1, 26.2_

- [ ] 2. Author docker-compose for local development
  - Compose `postgres`, `qdrant`, `mongo` with persisted volumes and exposed ports
  - Verify `docker compose up` brings all three up cleanly
  - _Requirements: 26.1_

- [ ] 3. Scaffold the Spring Boot project
  - Generate project (Java 21) with Web, Security, Data JPA, PostgreSQL driver, Flyway, Validation, springdoc-openapi
  - Configure datasource from environment variables; fail startup if a required var is missing
  - _Requirements: 24.1, 26.1, 26.2_

- [ ] 4. Create Flyway baseline migration for the full schema
  - Implement `V1__init.sql` from `docs/erd/schema.sql` (all 15 tables, constraints, indexes); money columns as BIGINT
  - Confirm migration runs on startup and recreates schema reproducibly
  - _Requirements: 26.1_

- [ ] 5. Implement global error handling and request logging
  - `GlobalExceptionHandler` returning `{timestamp, status, error, message, path}`
  - Request-logging filter (method, path, status, duration) excluding passwords/tokens/keys
  - _Requirements: 26.3, 27.1, 27.4_

- [ ] 6. Implement health endpoint
  - `GET /health` returns 200 when app + DB available, 503 otherwise
  - _Requirements: 27.2, 27.3_

- [ ] 7. Configure Swagger/OpenAPI with bearer security
  - Document all endpoints with schemas; add bearer security scheme and per-endpoint role annotations
  - _Requirements: 24.1, 24.2, 24.3_

- [ ] 8. Add seed data
  - Seed one ADMIN user, sample categories, ~12 books for local development, and knowledge-base sample docs for RAG (idempotent seed using INSERT ON CONFLICT DO NOTHING); note: SLA tests require Sample_Dataset_Size of 1000 books
  - _Requirements: 26.1_

- [ ] 9. Set up GitHub Actions CI
  - Build + test Spring Boot (Maven) and RAG (pytest) at root and /rag; failing checks block merge; secrets from repo/env, never committed (React frontend is in a separate repository and excluded from this pipeline)
  - _Requirements: 25.1, 25.2, 25.4_

- [ ] 10. Implement User entity, repository, and registration
  - Map `users`; create CUSTOMER on register; BCrypt hashing; unique email (409); password policy (400); email format (400)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 11. Implement JWT issuance and login/refresh
  - Login returns access (15m) + refresh (7d) with userId + role claims; invalid credentials → 401; refresh rotates; expired/invalid refresh → 401
  - Implement a daily scheduled cron job (or Spring `@Scheduled` task) to delete expired refresh tokens from the database
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

- [ ] 12. Implement JWT validation filter and RBAC
  - Validate bearer token (401 on missing/expired/malformed); admin routes require ADMIN (403 for CUSTOMER); catalog browse/search public
  - Filter checks User's enabled status from DB/cache on every incoming request to reject active access tokens for disabled users
  - Configure SecurityConfig to whitelist (permitAll) and disable CSRF for `/api/payment/webhook/**`, and configure CORS to allow incoming post callbacks
  - Implement a custom `AuthenticationEntryPoint` and `AccessDeniedHandler` registered in `SecurityConfig` to write the standard JSON error schema (`{timestamp, status, error, message, path}`) directly to the response output stream for all 401 and 403 errors, ensuring no default Spring Boot raw/empty responses are returned
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 15.3_

- [ ] 13. Implement profile management, address CRUD, and tier calculations
  - `GET /auth/me`, update profile, change password (verify current → 400 if wrong)
  - Implement dynamic tier calculation based on the user's `lifetime_points` column in the database (SILVER < 1k, GOLD 1k-5k, PLATINUM >= 5k)
  - Implement full CRUD endpoints for shipping addresses (GET, POST, PUT, DELETE /me/addresses)
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 14. Implement catalog listing with pagination
  - Paginated (default 10, max 50/page) with total count, page number, total pages; out-of-range page → empty list + correct total
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 15. Implement search, filter, and sort
  - Case-insensitive title/author search; filters category/author/price; reject min>max (400); combined filters AND; empty result → empty list
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [ ] 16. Implement book detail
  - Full detail incl. stock + rating; missing id → 404 empty body; zero-stock → detail with stock 0
  - _Requirements: 7.1, 7.2, 7.3_

- [ ] 17. Implement reviews and ratings
  - Create review (1–5, 400 if out of range); one per user/book (409 duplicate); list paginated; delete own (200) else 403; recompute `rating_avg`
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

- [ ] 18. Implement shopping cart and guest cart merge
  - Add/update/remove items; reject quantity > stock (400); compute subtotal; return items + subtotal
  - Implement guest cart merge on login (merge guest session cart to user cart, remove inactive books, sum quantities up to stock limit, discard guest prices, and resolve unit prices using current real-time catalog prices)
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

- [ ] 19. Implement checkout and generic payment integration
  - Validate non-empty cart (400) and active books (400); create PENDING order, hold stock atomically for 15m, and generate secure payment URL via PaymentServiceFactory (201); apply shipping fee logic (flat 30k VND, free if net order total >= 300k VND); handle Webhook callback (/api/payment/webhook/{provider}) to verify signature, transition to PAID, decrement stock/update sold_count atomically, and award points; transition to CANCELLED, restore stock atomically, and release voucher on failure/timeout.
  - VNPAY checkout URL must explicitly pass `vnp_ExpireDate` matching the 15-minute stock hold. Webhook callback and cancellation transitions must use database pessimistic write locking (e.g. `SELECT FOR UPDATE`) on the Order record to serialize concurrent status updates.
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9, 10.10, 10.11, 10.12_

- [ ] 19a. Implement Order timeout scheduler & webhook grace period
  - Create background OrderTimeoutScheduler to cancel PENDING orders after 15 minutes (timeout duration configurable in application properties) and restore stock/vouchers atomically; implement webhook check for authorization timestamp to resolve timeout race conditions.
  - _Requirements: 10.10, 10.11_

- [ ] 20. Implement order history, detail, and cancel
  - Paginated history (desc, default 10, max 50); detail with ownership check (403 others'); cancel PENDING/PAID → restore stock (200); reject SHIPPED/DELIVERED/CANCELLED (409)
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [ ] 21. Implement admin book management with soft-delete & async events
  - CRUD; reject negative price/stock (400), reject price=0 AND stock=0 (400); block delete if referenced by orders (409) and set active=false (soft-delete), removing book from all user cart_items; emit asynchronous BookChangedEvent for RAG reindexing.
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7_

- [ ] 22. Implement admin category management
  - CRUD; unique name (409 duplicate); block delete if referenced by a book (409)
  - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [ ] 23. Implement admin order management
  - List/filter by status; transitions PAID→SHIPPED→DELIVERED (200); invalid transition (409); implement admin refund/cancel after DELIVERED (deduct points, refund used points)
  - _Requirements: 14.1, 14.2, 14.3, 14.4_

- [ ] 24. Implement admin user management
  - List; change role; disable account (login then 403); block self role-change/disable (409)
  - _Requirements: 15.1, 15.2, 15.3, 15.4_

- [ ] 25. Implement statistics dashboard
  - Totals (orders, revenue excluding CANCELLED, users, books); top-5 selling books
  - _Requirements: 16.1, 16.2, 16.3, 16.4_

- [ ] 26. Implement voucher management, endpoints, and application
  - Admin CRUD vouchers; apply at checkout with per-user limit, usage limit, min-order, expiry; no stacking; record `voucher_redemptions`
  - Implement customer-facing endpoints: `POST /vouchers/apply` (apply/validate voucher for cart) and `GET /me/vouchers` (list user's applicable vouchers)
  - _Requirements: 10.6, 10.8_

- [ ] 27. Replace fake OpenAI adapters with real OpenAI (RAG service, Python)
  - Real embedding (text-embedding-3-small, dim 1536) and chat (gpt-4o-mini) behind the existing interface; keep fake for tests
  - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5_

- [ ] 28. Verify and harden ingestion pipeline (RAG service, Python)
  - Ingest splits/embeds/upserts to Qdrant with metadata; replace prior entries on re-ingest; record per-chunk failures + report counts; create Qdrant payload index on `book_id` for fast vector deletions
  - Use `pypdf`/`pymupdf` and `ebooklib` for text-only extraction, ignoring complex layouts/equations to avoid parsing time traps
  - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_

- [ ] 29. Implement retrieval and grounded answering (RAG service, Python)
  - Embed query; retrieve top_k (≤5 default); include sources in prompt; no chunk over threshold → answer "not in knowledge base"
  - _Requirements: 18.1, 18.2, 18.3, 18.4_

- [ ] 30. Implement Spring Boot AI integration endpoints
  - `POST /ai/chat` (auth required, 401 without token, 403 if role not permitted); persist conversation + messages; proxy to RAG `/query`; 504 on timeout; `GET /ai/conversations`; `DELETE /ai/conversations/{id}`
  - Enforce context length limit: select last 10 chronological user/assistant messages, excluding the separate prepended system instructions prompt
  - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6, 19.7_

- [ ] 31. Implement book recommendations grounded in catalog
  - Map RAG results to catalog books; return title/author/id; recommend only existing active books
  - _Requirements: 20.1, 20.2, 20.3_

- [ ] 32. Implement per-user AI rate limiting and reindex trigger
  - Per-user chat rate limit (20 req/min) → 429; admin `POST /admin/ai/reindex`; reindex on BookChangedEvent
  - _Requirements: 17.5, 21.3_

- [ ] 33. Enforce CORS, validation, and secret handling end to end
  - Restrict CORS to configured frontend origin; validate all bodies (400 invalid fields); all secrets from env vars
  - _Requirements: 26.1, 26.2, 26.3, 26.4_

- [ ] 34. Finalize CI/CD deployment
  - Deploy backend (Railway/Render) and RAG on merge to main; smoke-test `/health`
  - _Requirements: 25.3_

- [ ] 35. Property test: no overselling and money math
  - Concurrent checkout stock invariant; `total == max(0, subtotal-discount)+shipping_fee`
  - _Requirements: 9.2, 10.2, 10.5, 10.8_

- [ ] 36. Property test: voucher limits and price snapshot
  - Per-user/overall voucher limits, min-order, no over-discount, unit_price snapshot integrity
  - _Requirements: 10.8_

- [ ] 37. Property test: loyalty points and IDOR
  - Points credited on DELIVERED and reversed on cancel; cross-user access returns 403/404
  - _Requirements: 11.3, 11.4, 3.3, 8.7_

- [ ] 38. Property test: catalog-grounded recommendations and rate limit
  - Recommendations reference only existing active books; exceeding chat rate returns 429
  - _Requirements: 20.3, 19.6, 21.3_

- [ ] 39. Implement Loyalty Points Management
  - Implement delta SQL updates / Pessimistic Locking for users.points and users.lifetime_points; credit points on DELIVERED and reverse on cancel (updating both balance and lifetime points accordingly, capping at 0); handle points deduction at checkout (max 20% limit); enforce no points + voucher stacking; implement `GET /me/points` endpoint with paginated point_transactions. Ensure database CHECK constraints prevent negative user points and lifetime points.
  - _Requirements: 4.5, 10.8, 10.9, 11.6_

- [ ] 40. Implement Prompt Injection Defense
  - Segregate System Prompt from User Messages; filter User Messages for injection keywords (ignore prior instructions, system prompt); add chatbot output content filter.
  - _Requirements: 32.1, 32.2, 32.3_

- [ ] 41. Implement Asynchronous RAG Ingestion & Status Dashboard
  - Listen to BookChangedEvent asynchronously; call RAG service with exponential backoff retry (up to 3 times); implement reindex status endpoints `GET /admin/ai/reindex/status` for dashboard.
  - Add a scheduled reconciliation cron job that runs nightly in Spring Boot to reconcile PostgreSQL catalog state with Qdrant vector store state, correcting any desynchronization caused by failed async events.
  - _Requirements: 33.1, 33.2, 33.3_

## Task Dependency Graph

```json
{
  "waves": [
    { "wave": 1, "tasks": [1, 2, 3], "description": "Repo, compose, Spring Boot scaffold" },
    { "wave": 2, "tasks": [4, 5, 6, 7], "description": "Schema migration, error handling, health, Swagger" },
    { "wave": 3, "tasks": [8, 9, 10], "description": "Seed, CI, user registration" },
    { "wave": 4, "tasks": [11, 12], "description": "JWT issuance and validation/RBAC (setup gate complete)" },
    { "wave": 5, "tasks": [13, 14, 18, 21, 22, 27, 28], "description": "Parallel feature start across workstreams" },
    { "wave": 6, "tasks": [15, 16, 19, 19a, 23, 24, 26, 29], "description": "Catalog search, checkout, admin, retrieval" },
    { "wave": 7, "tasks": [17, 20, 25, 30, 31, 39, 40], "description": "Reviews, orders, stats, AI integration, security" },
    { "wave": 8, "tasks": [32, 33, 41], "description": "Rate limit/reindex, cross-cutting hardening, async status" },
    { "wave": 9, "tasks": [34, 35, 36, 37, 38], "description": "Deploy and property-based tests" }
  ]
}
```

## Notes

- Waves 1–4 are the "setup gate" (Leader + P1) and must complete before parallel feature work begins.
- WS5 splits into Python RAG tasks (27–29) and Spring Boot integration tasks (30–32).
- Money is integer VND everywhere. Each task is done only when it meets the Definition of Done in `docs/PROJECT-KICKOFF.md` (endpoint + validation + correct error schema + RBAC + tests + Swagger + runnable via docker-compose).
- Frontend tasks are intentionally excluded; they belong to the separate frontend repository.
