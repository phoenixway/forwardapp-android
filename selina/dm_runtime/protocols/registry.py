from __future__ import annotations

from pathlib import Path

from dm_runtime.protocols.loader import load_protocol
from dm_runtime.protocols.protocol_models import Protocol


class ProtocolRegistry:
    def __init__(self, protocols: dict[str, Protocol]):
        self._protocols = protocols

    @classmethod
    def from_dirs(cls, dirs: list[Path]) -> "ProtocolRegistry":
        protocols: dict[str, Protocol] = {}
        for directory in dirs:
            if not directory.exists():
                continue
            for pattern in ("*.md", "*.markdown", "*.yaml", "*.yml"):
                for path in sorted(directory.glob(pattern)):
                    protocol = load_protocol(path)
                    # Earlier directories win unless a later file has the same id.
                    # User protocol dir is later in default_protocol_dirs(), so it can override project defaults.
                    protocols[protocol.id] = protocol
        return cls(protocols)

    def get(self, protocol_id: str) -> Protocol | None:
        return self._protocols.get(protocol_id)

    def require(self, protocol_id: str) -> Protocol:
        protocol = self.get(protocol_id)
        if protocol is None:
            known = ", ".join(sorted(self._protocols)) or "none"
            raise KeyError(f"Unknown protocol '{protocol_id}'. Known protocols: {known}")
        return protocol

    def all(self) -> list[Protocol]:
        return [self._protocols[key] for key in sorted(self._protocols)]
