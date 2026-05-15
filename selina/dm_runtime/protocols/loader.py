from __future__ import annotations

import re
from pathlib import Path
from typing import Any

import yaml

from dm_runtime.protocols.protocol_models import Protocol, ProtocolStep


class ProtocolLoadError(RuntimeError):
    pass


_FRONTMATTER_RE = re.compile(r"\A---\s*\n(?P<yaml>.*?)\n---\s*\n?(?P<body>.*)\Z", re.DOTALL)


def load_protocol(path: Path) -> Protocol:
    if path.suffix.lower() in {".md", ".markdown"}:
        return _load_markdown_protocol(path)
    if path.suffix.lower() in {".yaml", ".yml"}:
        return _load_yaml_protocol(path)
    raise ProtocolLoadError(f"Unsupported protocol file type: {path}")


def _load_yaml_protocol(path: Path) -> Protocol:
    try:
        raw: dict[str, Any] = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    except Exception as exc:  # pragma: no cover
        raise ProtocolLoadError(f"Failed to load protocol {path}: {exc}") from exc
    return _protocol_from_mapping(raw, path, body_markdown="")


def _load_markdown_protocol(path: Path) -> Protocol:
    text = path.read_text(encoding="utf-8")
    match = _FRONTMATTER_RE.match(text)
    if not match:
        raise ProtocolLoadError(
            f"Markdown protocol {path} must start with YAML frontmatter delimited by ---"
        )

    try:
        raw: dict[str, Any] = yaml.safe_load(match.group("yaml")) or {}
    except Exception as exc:  # pragma: no cover
        raise ProtocolLoadError(f"Failed to parse frontmatter in {path}: {exc}") from exc

    body = match.group("body").strip()
    return _protocol_from_mapping(raw, path, body_markdown=body)


def _protocol_from_mapping(raw: dict[str, Any], path: Path, body_markdown: str) -> Protocol:
    missing = [key for key in ("id", "title", "kind") if key not in raw]
    if missing:
        raise ProtocolLoadError(f"Protocol {path} is missing required keys: {missing}")

    description = str(raw.get("description") or _description_from_markdown(body_markdown) or "")
    steps = [ProtocolStep(**step) for step in raw.get("steps", [])]

    return Protocol(
        id=str(raw["id"]),
        title=str(raw["title"]),
        kind=str(raw["kind"]),
        description=description,
        steps=steps,
        applies_when=dict(raw.get("applies_when", {})),
        outputs=dict(raw.get("outputs", {})),
        source_path=path,
        body_markdown=body_markdown,
    )


def _description_from_markdown(body: str) -> str:
    for block in body.split("\n\n"):
        cleaned = block.strip()
        if not cleaned or cleaned.startswith("#"):
            continue
        return re.sub(r"\s+", " ", cleaned)
    return ""
