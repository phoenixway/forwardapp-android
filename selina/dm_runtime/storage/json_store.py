from __future__ import annotations

import json
from dataclasses import asdict
from datetime import date
from pathlib import Path

from dm_runtime.domain.models import DayState, RuntimeEvent
from dm_runtime.domain.serialization import state_from_dict, state_to_dict
from dm_runtime.storage.paths import APP_DIR, EVENT_LOG_FILE, STATE_FILE


class JsonStateStore:
    def __init__(self, state_file: Path = STATE_FILE):
        self.state_file = state_file

    def load(self) -> DayState:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        today = date.today().isoformat()
        if not self.state_file.exists():
            return DayState(date=today)
        data = json.loads(self.state_file.read_text(encoding="utf-8"))
        if data.get("date") != today:
            return DayState(date=today)
        return state_from_dict(data)

    def save(self, state: DayState) -> None:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        self.state_file.write_text(
            json.dumps(state_to_dict(state), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )


class JsonlEventLog:
    def __init__(self, event_log_file: Path = EVENT_LOG_FILE):
        self.event_log_file = event_log_file

    def append_many(self, events: list[RuntimeEvent]) -> None:
        if not events:
            return
        APP_DIR.mkdir(parents=True, exist_ok=True)
        with self.event_log_file.open("a", encoding="utf-8") as f:
            for event in events:
                f.write(json.dumps(asdict(event), ensure_ascii=False) + "\n")
