from __future__ import annotations

from rag.config import settings
from rag.schemas import QueryResponse, SearchHit, Source
from rag.services import FakeOpenAIService, MongoBookStore, QdrantStore


class RagEngine:
    def __init__(
        self,
        openai_service: FakeOpenAIService | None = None,
        manifest: MongoBookStore | None = None,
        store: QdrantStore | None = None,
    ) -> None:
        self.openai_service = openai_service or FakeOpenAIService()
        self.manifest = manifest or MongoBookStore()
        self.store = store or QdrantStore()

    def query(
        self,
        query: str,
        document_name: str | None = None,
        top_k: int | None = None,
    ) -> QueryResponse:
        limit = min(top_k or settings.default_top_k, settings.max_top_k)
        vector = self.openai_service.embed_texts([query])[0]
        search_limit = settings.qdrant_query_limit if document_name else limit
        hits = self.store.search(vector=vector, limit=search_limit)
        sources = self._sources_from_hits(hits, document_name=document_name)[:limit]
        answer, usage = self.openai_service.make_answer(query, sources)
        return QueryResponse(answer=answer, sources=sources, usage=usage)

    def _sources_from_hits(
        self,
        hits: list[SearchHit],
        document_name: str | None = None,
    ) -> list[Source]:
        sources: list[Source] = []
        for hit in hits:
            chunk = self.manifest.get_chunk(hit.id)
            if chunk is None:
                continue
            if document_name and chunk.get("document_name") != document_name:
                continue
            sources.append(_source_from_hit(hit.score, chunk))
        return sources


def _source_from_hit(score: float, payload: dict[str, object]) -> Source:
    text = str(payload.get("text") or "")
    return Source(
        document_name=str(payload.get("document_name") or ""),
        file_name=str(payload.get("file_name") or ""),
        file_type=str(payload.get("file_type") or ""),
        chunk_index=int(payload.get("chunk_index") or 0),
        page=_optional_int(payload.get("page")),
        score=score,
        text=text,
    )


def _optional_int(value: object) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None
