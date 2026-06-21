from types import SimpleNamespace

from rag.ingestion import TextChunk
from rag.services import QdrantStore


class FakeQdrantClient:
    def __init__(self):
        self.deleted = []
        self.upserted = []
        self.payload_indexes = []

    def get_collection(self, collection_name):
        return SimpleNamespace()

    def create_payload_index(self, **kwargs):
        self.payload_indexes.append(kwargs)

    def delete(self, **kwargs):
        self.deleted.append(kwargs)

    def upsert(self, **kwargs):
        self.upserted.append(kwargs)

    def query_points(self, **kwargs):
        assert kwargs["query"] == [0.1, 0.2]
        return SimpleNamespace(
            points=[
                SimpleNamespace(
                    id="chunk-1",
                    score=0.75,
                    payload={"content": "chunk text", "book_id": "book-id"},
                )
            ]
        )


def test_qdrant_store_uses_point_id_and_content_only():
    client = FakeQdrantClient()
    store = QdrantStore(client=client, collection_name="books")
    chunk = TextChunk(
        id="chunk-1",
        document_name="Sample",
        file_name="Sample.epub",
        file_type="epub",
        chunk_index=0,
        text="chunk text",
        book_id="book-id",
    )

    store.ensure_collection()
    store.delete_points(["old-chunk"])
    store.upsert_chunks([chunk], [[0.1, 0.2]])
    results = store.search([0.1, 0.2], limit=1)

    assert client.payload_indexes
    assert client.payload_indexes[0]["field_name"] == "book_id"
    assert client.deleted
    point = client.upserted[0]["points"][0]
    assert point.id == "chunk-1"
    assert point.payload["content"] == "chunk text"
    assert point.payload["book_id"] == "book-id"
    assert results[0].id == "chunk-1"
