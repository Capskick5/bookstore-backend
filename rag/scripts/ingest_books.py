from __future__ import annotations

import argparse
import sys
import time

from rag.ingestion import IngestionPipeline, resolve_book_paths


def main() -> int:
    configure_stdio()

    parser = argparse.ArgumentParser(description="Ingest books with per-file progress.")
    parser.add_argument(
        "documents",
        nargs="*",
        help="Optional document names. Omit to ingest every supported file in assets/books.",
    )
    args = parser.parse_args()

    documents = args.documents or None
    paths, missing = resolve_book_paths(documents)
    pipeline = IngestionPipeline()

    print(f"missing={len(missing)}", flush=True)
    for name in missing:
        print(f"MISSING {name}", flush=True)

    total = len(paths)
    print(f"total={total}", flush=True)

    indexed_count = 0
    error_count = 0
    total_chunks = 0
    started_at = time.perf_counter()

    for index, path in enumerate(paths, start=1):
        item_started_at = time.perf_counter()
        print(f"[{index}/{total}] START {path.name}", flush=True)

        response = pipeline.ingest(documents=[path.name])
        elapsed = time.perf_counter() - item_started_at

        for document in response.indexed:
            indexed_count += 1
            total_chunks += document.chunks
            print(
                f"[{index}/{total}] OK {document.file_name} chunks={document.chunks} elapsed={elapsed:.1f}s",
                flush=True,
            )

        for error in response.errors:
            error_count += 1
            print(
                f"[{index}/{total}] ERROR {error.document}: {error.error} elapsed={elapsed:.1f}s",
                flush=True,
            )

    elapsed_total = time.perf_counter() - started_at
    print(
        f"DONE indexed={indexed_count} errors={error_count} total_chunks={total_chunks} elapsed={elapsed_total:.1f}s",
        flush=True,
    )
    return 1 if missing or error_count else 0


def configure_stdio() -> None:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")


if __name__ == "__main__":
    raise SystemExit(main())
