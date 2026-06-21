from fastapi.testclient import TestClient

from rag.api import create_app, get_ingestion_pipeline, get_manifest, get_store
from rag.schemas import IndexedDocument, IngestResponse, SearchHit


class FakePipeline:
    def ingest(self, documents=None):
        return IngestResponse(
            indexed=[
                IndexedDocument(
                    document_name="Sample",
                    file_name="Sample.epub",
                    chunks=1,
                )
            ],
            total_chunks=1,
        )


class FakeManifest:
    database_name = "books_rag"
    books_collection_name = "books"
    chunks_collection_name = "chunks"
    images_collection_name = "images"

    def is_available(self):
        return True

    def get_chunk(self, chunk_id):
        return {
            "id": chunk_id,
            "document_name": "Sample",
            "file_name": "Sample.epub",
            "file_type": "epub",
            "chunk_index": 0,
            "page": 1,
            "text": "A useful source chunk.",
        }


class FakeStore:
    collection_name = "books"

    def is_available(self):
        return True

    def search(self, vector, limit):
        return [SearchHit(id="chunk-1", score=0.9, payload={"content": "A useful source chunk."})]


def test_api_smoke_endpoints():
    app = create_app()
    app.dependency_overrides[get_manifest] = lambda: FakeManifest()
    app.dependency_overrides[get_store] = lambda: FakeStore()
    app.dependency_overrides[get_ingestion_pipeline] = lambda: FakePipeline()
    client = TestClient(app)

    health = client.get("/health")
    assert health.status_code == 200
    assert health.json()["qdrant"] == "ok"
    assert health.json()["mongo"] == "ok"

    ingest = client.post("/ingest")
    assert ingest.status_code == 200
    assert ingest.json()["total_chunks"] == 1

    query = client.post("/query", json={"query": "What is inside?", "top_k": 1})
    assert query.status_code == 200
    assert query.json()["sources"][0]["file_name"] == "Sample.epub"

def test_query_below_similarity_threshold_returns_not_in_kb_message():
    class LowScoreStore:
        collection_name = "books"

        def is_available(self):
            return True

        def search(self, vector, limit):
            return [SearchHit(id="chunk-1", score=0.1, payload={"content": "irrelevant"})]

    app = create_app()
    app.dependency_overrides[get_manifest] = lambda: FakeManifest()
    app.dependency_overrides[get_store] = lambda: LowScoreStore()
    client = TestClient(app)

    response = client.post("/query", json={"query": "What is inside?", "top_k": 1})

    assert response.status_code == 200
    assert response.json()["sources"] == []
    assert "knowledge base does not contain" in response.json()["answer"].lower()


def test_v1_endpoints_are_not_public():
    app = create_app()
    app.dependency_overrides[get_manifest] = lambda: FakeManifest()
    app.dependency_overrides[get_store] = lambda: FakeStore()
    client = TestClient(app)

    assert client.post("/v1/embeddings", json={"input": "hello"}).status_code == 404
    assert client.post("/v1/chat/completions", json={"messages": []}).status_code == 404
