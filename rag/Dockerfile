# syntax=docker/dockerfile:1

FROM python:3.13-slim AS runtime
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY pyproject.toml README.md ./
COPY rag ./rag
COPY main.py ./

RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir .

EXPOSE 8000
CMD ["uvicorn", "rag.api:app", "--host", "0.0.0.0", "--port", "8000"]
