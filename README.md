# BookVerse — Backend & AI

Online bookstore backend with a RAG-powered AI assistant.

- **Spring Boot** REST API + **PostgreSQL** (business domain: auth, catalog, cart, orders, admin, vouchers, loyalty points)
- **Python RAG service** (`/rag`, FastAPI) + **Qdrant** + **MongoDB** + **OpenAI** (book ingestion, retrieval, answering)
- Frontend (React) lives in a separate repository.

## Cấu trúc

```
.
├── bookstore/  # Spring Boot application (Java 21)
├── rag/        # Python RAG microservice (FastAPI)
├── infra/      # docker-compose (postgres + qdrant + mongo)
└── docs/       # project documentation
```

## Chạy local

Hướng dẫn chi tiết cho thành viên mới: `docs/SETUP-GUIDE.md`.

```bash
# 1. Khởi động database
docker compose -f infra/docker-compose.yml up -d

# 2. Tạo file môi trường
cp .env.example .env   # rồi điền giá trị

# 3. Chạy backend
cd bookstore && ./mvnw spring-boot:run

# 4. (tùy chọn) chạy RAG service
cd rag && uv sync && uv run main.py
```

- API docs (Swagger): `http://localhost:8080/swagger-ui`
- Health check: `http://localhost:8080/api/health`

## Quy ước nhóm

- Một nhánh `main`; commit nhỏ, thường xuyên; `git pull --rebase` trước khi push.
- Không commit `.env` hay API key.
- Mỗi tính năng phải đạt Definition of Done (validation, phân quyền, test, Swagger).

> Tài liệu chi tiết (kế hoạch, ERD, phân công) được lưu trên Notion của nhóm.
