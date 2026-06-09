from pathlib import Path
from types import SimpleNamespace

from rag.ingestion import IngestionPipeline, PageText, ParsedDocument, resolve_book_paths


class FakeOpenAI:
    def embed_texts(self, texts):
        return [[1.0, 0.0] for _text in texts]


class FakeManifest:
    def __init__(self):
        self.indexed = []
        self.errors = []
        self.old_ids = ["old-chunk"]

    def book_id_for_path(self, path):
        return "book-id"

    def chunk_ids_for_book(self, book_id):
        return self.old_ids

    def ensure_indexes(self):
        pass

    def mark_indexed(self, **kwargs):
        self.indexed.append(kwargs)

    def mark_error(self, **kwargs):
        self.errors.append(kwargs)


class FakeStore:
    def __init__(self):
        self.ensure_count = 0
        self.deleted = []
        self.upserts = []

    def ensure_collection(self):
        self.ensure_count += 1

    def delete_points(self, point_ids):
        self.deleted.append(point_ids)

    def upsert_chunks(self, chunks, vectors):
        self.upserts.append((chunks, vectors))


def test_reingesting_same_document_replaces_json_entry(monkeypatch, tmp_path: Path):
    book_path = tmp_path / "Sample.epub"
    book_path.write_text("placeholder", encoding="utf-8")

    import rag.ingestion as ingestion_module

    monkeypatch.setattr(
        ingestion_module,
        "settings",
        SimpleNamespace(
            books_dir=tmp_path,
            chunk_target_tokens=300,
            chunk_overlap_tokens=100,
        ),
    )
    monkeypatch.setattr(
        ingestion_module,
        "parse_book",
        lambda _path: ParsedDocument(
            document_name="Sample",
            file_name="Sample.epub",
            file_type="epub",
            pages=[PageText(text="one two three four five six")],
        ),
    )

    manifest = FakeManifest()
    store = FakeStore()
    pipeline = IngestionPipeline(
        openai_service=FakeOpenAI(),
        manifest=manifest,
        store=store,
    )

    first = pipeline.ingest(["Sample"])
    second = pipeline.ingest(["Sample"])

    assert first.total_chunks == 1
    assert second.total_chunks == 1
    assert len(manifest.indexed) == 2
    assert len(manifest.indexed[0]["chunks"]) == 1
    assert store.deleted == [["old-chunk"], ["old-chunk"]]
    assert len(store.upserts) == 2
    assert store.upserts[0][1] == [[1.0, 0.0]]


def test_resolve_book_paths_accepts_sanitized_document_name(monkeypatch, tmp_path: Path):
    book_path = tmp_path / "Messy_Name_ (z-library.sk, 1lib.sk).pdf"
    book_path.write_text("placeholder", encoding="utf-8")

    import rag.ingestion as ingestion_module

    monkeypatch.setattr(
        ingestion_module,
        "settings",
        SimpleNamespace(books_dir=tmp_path),
    )

    paths, missing = resolve_book_paths(["Messy Name"])

    assert paths == [book_path]
    assert missing == []
