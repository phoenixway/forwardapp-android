from __future__ import annotations

from pathlib import Path


APP_DIR = Path.home() / ".dm_runtime"
STATE_FILE = APP_DIR / "current_day.json"
EVENT_LOG_FILE = APP_DIR / "event_log.jsonl"
CONFIG_FILE = APP_DIR / "config.yaml"
GOALS_FILE = APP_DIR / "current_goals.md"
USER_PROTOCOLS_DIR = APP_DIR / "protocols"
PROJECT_PROTOCOLS_DIR = Path.cwd() / "protocols"


def default_protocol_dirs() -> list[Path]:
    return [PROJECT_PROTOCOLS_DIR, USER_PROTOCOLS_DIR]
