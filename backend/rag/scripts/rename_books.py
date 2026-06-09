from __future__ import annotations

import argparse
import re
import sys
import unicodedata
from pathlib import Path


DEFAULT_BOOK_DIR = Path("assets/books")
DEFAULT_EXTENSIONS = {".pdf", ".epub", ".mobi", ".azw3", ".txt"}

SOURCE_TAG_RE = re.compile(
    r"\s*[\(\[][^)\]]*(?:z-library|z-lib|1lib|libgen)[^)\]]*[\)\]]\s*",
    re.IGNORECASE,
)
WHITESPACE_RE = re.compile(r"\s+")
UNDERSCORE_RE = re.compile(r"_+")
UNSAFE_RE = re.compile(r"[^A-Za-z0-9._-]+")


def main() -> int:
    configure_stdio()

    parser = argparse.ArgumentParser(
        description="Sanitize book filenames: remove Vietnamese accents, source tags, and replace spaces with underscores.",
    )
    parser.add_argument(
        "directory",
        nargs="?",
        type=Path,
        default=DEFAULT_BOOK_DIR,
        help=f"Book directory to scan. Default: {DEFAULT_BOOK_DIR}",
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Actually rename files. Without this flag, only prints a dry-run preview.",
    )
    parser.add_argument(
        "--recursive",
        action="store_true",
        help="Scan subdirectories recursively.",
    )
    parser.add_argument(
        "--ext",
        action="append",
        default=[],
        help="Allowed extension, e.g. --ext .pdf --ext .epub. Defaults to common book formats.",
    )
    args = parser.parse_args()

    directory = args.directory.resolve()
    if not directory.is_dir():
        raise SystemExit(f"Directory not found: {directory}")

    extensions = normalize_extensions(args.ext) if args.ext else DEFAULT_EXTENSIONS
    files = iter_book_files(directory, extensions, recursive=args.recursive)
    renames = build_rename_plan(files)

    if not renames:
        print("No files need renaming.")
        return 0

    print("Rename plan:")
    for source, target in renames:
        print(f"  {source.relative_to(directory)} -> {target.relative_to(directory)}")

    if not args.apply:
        print("\nDry run only. Add --apply to rename files.")
        return 0

    for source, target in renames:
        source.rename(target)

    print(f"\nRenamed {len(renames)} file(s).")
    return 0


def configure_stdio() -> None:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")


def normalize_extensions(values: list[str]) -> set[str]:
    return {
        value.lower() if value.startswith(".") else f".{value.lower()}"
        for value in values
    }


def iter_book_files(directory: Path, extensions: set[str], *, recursive: bool) -> list[Path]:
    pattern = "**/*" if recursive else "*"
    return sorted(
        path
        for path in directory.glob(pattern)
        if path.is_file() and path.suffix.lower() in extensions
    )


def build_rename_plan(files: list[Path]) -> list[tuple[Path, Path]]:
    occupied = {path.resolve() for path in files}
    planned: set[Path] = set()
    renames: list[tuple[Path, Path]] = []

    for source in files:
        sanitized_name = sanitize_filename(source.name)
        target = source.with_name(sanitized_name)
        target = unique_target(target, occupied, planned, source.resolve())

        if target.name == source.name:
            continue

        renames.append((source, target))
        planned.add(target.resolve())

    return renames


def unique_target(
    target: Path,
    occupied: set[Path],
    planned: set[Path],
    source_resolved: Path,
) -> Path:
    candidate = target
    index = 2

    while True:
        candidate_resolved = candidate.resolve()
        conflicts_with_existing = (
            candidate_resolved in occupied and candidate_resolved != source_resolved
        )
        conflicts_with_plan = candidate_resolved in planned

        if not conflicts_with_existing and not conflicts_with_plan:
            return candidate

        candidate = target.with_name(f"{target.stem}_{index}{target.suffix}")
        index += 1


def sanitize_filename(filename: str) -> str:
    path = Path(filename)
    stem = sanitize_stem(path.stem)
    suffix = sanitize_suffix(path.suffix)
    return f"{stem}{suffix}"


def sanitize_stem(stem: str) -> str:
    text = unicodedata.normalize("NFKC", stem)
    text = SOURCE_TAG_RE.sub(" ", text)
    text = strip_vietnamese_accents(text)
    text = text.replace("&", " and ")
    text = UNSAFE_RE.sub(" ", text)
    text = WHITESPACE_RE.sub("_", text)
    text = UNDERSCORE_RE.sub("_", text)
    text = text.strip("._-")
    return text or "Untitled"


def sanitize_suffix(suffix: str) -> str:
    return strip_vietnamese_accents(suffix).lower()


def strip_vietnamese_accents(value: str) -> str:
    value = value.replace("Đ", "D").replace("đ", "d")
    normalized = unicodedata.normalize("NFD", value)
    return "".join(
        char
        for char in normalized
        if unicodedata.category(char) != "Mn" and ord(char) < 128
    )


if __name__ == "__main__":
    raise SystemExit(main())
