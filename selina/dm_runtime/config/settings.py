from __future__ import annotations

from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

import yaml

from dm_runtime.storage.paths import APP_DIR, CONFIG_FILE


@dataclass
class RuntimeSettings:
    wake_time: str = "08:00"
    no_plan_after_wake_minutes: int = 180
    max_preparation_minutes: int = 120
    watch_interval_seconds: int = 60
    alarm_on_triggers: bool = True
    notification_backend: str = "auto"
    notification_repeat_minutes: int = 15


class SettingsStore:
    def __init__(self, config_file: Path = CONFIG_FILE):
        self.config_file = config_file

    def ensure_exists(self) -> None:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        if not self.config_file.exists():
            self.config_file.write_text(yaml.safe_dump(asdict(RuntimeSettings()), sort_keys=False), encoding="utf-8")

    def load(self) -> RuntimeSettings:
        self.ensure_exists()
        raw: dict[str, Any] = yaml.safe_load(self.config_file.read_text(encoding="utf-8")) or {}
        defaults = asdict(RuntimeSettings())
        defaults.update(raw)
        return RuntimeSettings(**defaults)

    def save(self, settings: RuntimeSettings) -> None:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        self.config_file.write_text(yaml.safe_dump(asdict(settings), sort_keys=False), encoding="utf-8")
