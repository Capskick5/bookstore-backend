# BookVerse Cloud Deployment Checklist

Use this checklist when deploying BookVerse to production.

## Backend (Spring Boot)

1. Set required environment variables (see `.env.example` and `application-prod.yml`).
2. Build container from `bookstore/Dockerfile` or deploy via Railway using `railway.toml`.
3. Run Flyway migrations against the production PostgreSQL instance.
4. Verify `GET /api/health` returns `status: UP` and database `UP`.

## RAG service (FastAPI)

1. Build from `rag/Dockerfile`.
2. Configure `OPENAI_API_KEY`, `QDRANT_URL`, `MONGO_URL`, and related variables.
3. Verify `GET /health` on the RAG service.

## Frontend (Vite + React)

1. Set `VITE_API_BASE_URL` to the production backend URL including `/api`.
2. Deploy static build to Vercel (`vercel.json` handles SPA routing).
3. Confirm admin dashboard, checkout, and chatbot can reach the API.

## Post-deploy smoke test

Run the manual GitHub Actions workflow **Deploy Smoke Test** with your backend URL, or locally:

```bash
curl -f https://your-backend.example.com/api/health
curl -f "https://your-backend.example.com/api/books?page=0&size=1"
```

## Security notes

- Never commit `.env` files.
- Disable Swagger in production (`spring.profiles.active=prod`).
- Rotate `JWT_SECRET` and payment signing secrets per environment.
