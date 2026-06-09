from __future__ import annotations

import uvicorn
from fastapi import Body, Depends, FastAPI

from rag.ingestion import IngestionPipeline
from rag.rag_engine import RagEngine
from rag.schemas import (
    HealthResponse,
    IngestRequest,
    IngestResponse,
    QueryRequest,
    QueryResponse,
)
from rag.services import FakeOpenAIService, MongoBookStore, QdrantStore


def get_manifest() -> MongoBookStore:
    return MongoBookStore()


def get_store() -> QdrantStore:
    return QdrantStore()


def get_openai_service() -> FakeOpenAIService:
    return FakeOpenAIService()


def get_rag_engine(
    openai_service: FakeOpenAIService = Depends(get_openai_service),
    manifest: MongoBookStore = Depends(get_manifest),
    store: QdrantStore = Depends(get_store),
) -> RagEngine:
    return RagEngine(openai_service=openai_service, manifest=manifest, store=store)


def get_ingestion_pipeline(
    openai_service: FakeOpenAIService = Depends(get_openai_service),
    manifest: MongoBookStore = Depends(get_manifest),
    store: QdrantStore = Depends(get_store),
) -> IngestionPipeline:
    return IngestionPipeline(openai_service=openai_service, manifest=manifest, store=store)


def create_app() -> FastAPI:
    app = FastAPI(title="Books RAG", version="0.1.0")

    @app.get("/health", response_model=HealthResponse)
    def health(
        manifest: MongoBookStore = Depends(get_manifest),
        store: QdrantStore = Depends(get_store),
    ) -> HealthResponse:
        return HealthResponse(
            status="ok",
            qdrant="ok" if store.is_available() else "unavailable",
            mongo="ok" if manifest.is_available() else "unavailable",
            mongo_database=manifest.database_name,
            books_collection=manifest.books_collection_name,
            chunks_collection=manifest.chunks_collection_name,
            images_collection=manifest.images_collection_name,
            collection=store.collection_name,
        )

    @app.post("/ingest", response_model=IngestResponse)
    def ingest(
        pipeline: IngestionPipeline = Depends(get_ingestion_pipeline),
        request: IngestRequest | None = Body(default=None),
    ) -> IngestResponse:
        documents = request.documents if request else None
        return pipeline.ingest(documents=documents)

    @app.post("/query", response_model=QueryResponse)
    def query(
        request: QueryRequest,
        engine: RagEngine = Depends(get_rag_engine),
    ) -> QueryResponse:
        return engine.query(
            query=request.query,
            document_name=request.document_name,
            top_k=request.top_k,
        )

    return app


app = create_app()


def run() -> None:
    uvicorn.run("rag.api:app", host="0.0.0.0", port=8000, reload=True)
