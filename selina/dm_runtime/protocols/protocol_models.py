from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class ProtocolStep:
    id: str
    title: str
    prompt: str | None = None
    instruction: str | None = None


@dataclass(frozen=True)
class Protocol:
    id: str
    title: str
    kind: str
    description: str
    steps: list[ProtocolStep] = field(default_factory=list)
    applies_when: dict[str, Any] = field(default_factory=dict)
    outputs: dict[str, Any] = field(default_factory=dict)
    source_path: Path | None = None
    body_markdown: str = ""

    @property
    def recommended_next_command(self) -> str | None:
        value = self.outputs.get("recommended_next_command")
        return str(value) if value else None
