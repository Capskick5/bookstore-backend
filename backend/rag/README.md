# RAG Service (Python · FastAPI)

Microservice xử lý phần AI: ingest sách (PDF/EPUB) → Qdrant + MongoDB; truy vấn → trả lời có dẫn nguồn (OpenAI). Spring Boot gọi service này qua HTTP nội bộ.

> **Lưu ý:** database container (Qdrant + MongoDB) đã nằm trong `infra/docker-compose.yml` ở root. KHÔNG dùng compose riêng trong thư mục này.

## Cấu trúc
```
rag/
├── main.py             # entrypoint (uvicorn)
├── pyproject.toml      # phụ thuộc (uv)
├── rag/                # mã nguồn (api, ingestion, rag_engine, services...)
├── scripts/            # script ingest/đổi tên
└── tests/              # pytest
```

## Chạy local

```bash
# 1. Bật DB từ root repo (nếu chưa)
docker compose -f ../infra/docker-compose.yml up -d

# 2. Cài phụ thuộc bằng uv
uv sync

# 3. Chạy service
uv run main.py
```

Service mở ở `http://localhost:8000`.

## Endpoints
- `GET /health` — kiểm tra Qdrant + Mongo.
- `POST /ingest` — ingest sách trong `assets/books/` vào Qdrant + Mongo.
- `POST /query {query, top_k?}` — tìm + sinh câu trả lời, trả `{answer, sources[], usage}`.

## Việc cần làm (P5)
- Thay `FakeOpenAIService` bằng OpenAI thật (text-embedding-3-small + gpt-4o-mini).
- Đảm bảo reindex khi sách trong catalog thay đổi.
- Đảm bảo gợi ý chỉ ra sách có thật trong catalog.

## Dữ liệu sách
Sách (PDF/EPUB) tải từ Google Drive nhóm vào `rag/assets/books/`. Thư mục này KHÔNG commit lên GitHub (đã chặn trong `.gitignore`).
