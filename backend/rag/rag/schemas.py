from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class Usage(BaseModel):
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0


class HealthResponse(BaseModel):
    status: Literal["ok"]
    qdrant: Literal["ok", "unavailable"]
    mongo: Literal["ok", "unavailable"]
    mongo_database: str
    books_collection: str
    chunks_collection: str
    images_collection: str
    collection: str


class IngestRequest(BaseModel):
    documents: list[str] | None = None


class IndexedDocument(BaseModel):
    document_name: str
    file_name: str
    chunks: int


class IngestError(BaseModel):
    document: str
    error: str


class IngestResponse(BaseModel):
    indexed: list[IndexedDocument] = Field(default_factory=list)
    errors: list[IngestError] = Field(default_factory=list)
    total_chunks: int = 0


class QueryRequest(BaseModel):
    query: str = Field(min_length=1)
    document_name: str | None = None
    top_k: int | None = Field(default=None, ge=1)


class Source(BaseModel):
    document_name: str
    file_name: str
    file_type: str
    chunk_index: int
    page: int | None = None
    score: float
    text: str


class QueryResponse(BaseModel):
    answer: str
    sources: list[Source]
    usage: Usage


class SearchHit(BaseModel):
    id: str
    score: float
    payload: dict[str, object]
