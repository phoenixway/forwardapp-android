from __future__ import annotations

import os
import re
import subprocess
from dataclasses import dataclass, replace
from pathlib import Path

from dm_runtime.domain.models import DayGoal, DayState
from dm_runtime.goals.markdown_codec import (
    GoalDocument,
    MinorActivity,
    ParkingItem,
    default_goals_markdown,
    parse_goals_markdown,
    render_goals_markdown,
)
from dm_runtime.storage.paths import APP_DIR, GOALS_FILE


ARCHIVE_GOALS_RE = re.compile(r"^goals-(?P<date>\d{4}-\d{2}-\d{2})(?:-(?P<copy>\d+))?\.md$")
UNFINISHED_STATUSES = {"OPEN", "ACTIVE", "APPROVED", "IN_PROGRESS", "BLOCKED"}
FINISHED_STATUSES = {"DONE", "DROPPED"}


@dataclass(frozen=True)
class CarryoverGoal:
    index: int
    source_date: str
    source_path: Path
    source_goal_id: str
    title: str
    status: str
    priority: int | None = None
    planned_minutes: int | None = None


class GoalStore:
    def __init__(self, goals_file: Path = GOALS_FILE):
        self.goals_file = goals_file

    def ensure_exists(self) -> None:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        if not self.goals_file.exists():
            self.goals_file.write_text(default_goals_markdown(), encoding="utf-8")

    def load(self) -> GoalDocument:
        self.ensure_exists()
        return parse_goals_markdown(self.goals_file.read_text(encoding="utf-8"))

    def save(self, doc: GoalDocument) -> None:
        APP_DIR.mkdir(parents=True, exist_ok=True)
        self.goals_file.write_text(render_goals_markdown(doc), encoding="utf-8")

    def add_goal(self, title: str, planned_minutes: int | None = None) -> DayGoal:
        doc = self.load()
        goal = DayGoal(
            id=doc.next_goal_id(),
            title=title.strip(),
            status="OPEN",
            priority=self._next_priority(doc),
            planned_minutes=planned_minutes,
        )
        doc.goals.append(goal)
        self.save(doc)
        return goal

    def add_inbox(self, title: str) -> ParkingItem:
        doc = self.load()
        item = ParkingItem(title=title.strip(), status="OPEN")
        doc.parking_lot.append(item)
        self.save(doc)
        return item

    def add_minor(self, title: str, minutes: int | None = None) -> MinorActivity:
        doc = self.load()
        item = MinorActivity(id=doc.next_minor_id(), title=title.strip(), status="OPEN", time_limit_minutes=minutes)
        doc.minor_activities.append(item)
        self.save(doc)
        return item

    def archive_and_reset(self, date_label: str) -> Path | None:
        """Archive current editable goals file and start a fresh one for a new day."""
        APP_DIR.mkdir(parents=True, exist_ok=True)
        archive_dir = APP_DIR / "archive"
        archive_dir.mkdir(parents=True, exist_ok=True)
        archived: Path | None = None
        if self.goals_file.exists():
            text = self.goals_file.read_text(encoding="utf-8")
            if text.strip():
                archived = archive_dir / f"goals-{date_label}.md"
                counter = 2
                while archived.exists():
                    archived = archive_dir / f"goals-{date_label}-{counter}.md"
                    counter += 1
                archived.write_text(text, encoding="utf-8")
        self.goals_file.write_text(default_goals_markdown(), encoding="utf-8")
        return archived

    def carryover_goals(self) -> list[CarryoverGoal]:
        """Return unfinished goals from archived day goal documents.

        This is intentionally derived from archive files every time, so it works
        both inside the REPL and from one-shot shell commands without relying on
        hidden session memory.
        """
        archive_dir = APP_DIR / "archive"
        if not archive_dir.exists():
            return []

        archive_entries: list[tuple[str, int, Path]] = []
        for path in archive_dir.glob("goals-*.md"):
            match = ARCHIVE_GOALS_RE.match(path.name)
            if not match:
                continue
            copy = int(match.group("copy") or "1")
            archive_entries.append((match.group("date"), copy, path))

        # Newest day first. For same date, newest copy first.
        archive_entries.sort(key=lambda item: (item[0], item[1], item[2].name), reverse=True)

        results: list[CarryoverGoal] = []
        for source_date, _copy, path in archive_entries:
            doc = parse_goals_markdown(path.read_text(encoding="utf-8"))
            for goal in doc.goals:
                if goal.status in FINISHED_STATUSES:
                    continue
                results.append(
                    CarryoverGoal(
                        index=len(results) + 1,
                        source_date=source_date,
                        source_path=path,
                        source_goal_id=goal.id,
                        title=goal.title,
                        status=goal.status,
                        priority=goal.priority,
                        planned_minutes=goal.planned_minutes,
                    )
                )
        return results

    def import_carryover(self, refs: str) -> list[DayGoal]:
        carry = self.carryover_goals()
        selected = self._select_carryover(carry, refs)
        if not selected:
            return []

        doc = self.load()
        existing_keys = {self._goal_duplicate_key(goal) for goal in doc.goals if goal.status not in FINISHED_STATUSES}
        imported: list[DayGoal] = []
        for item in selected:
            goal = DayGoal(
                id=doc.next_goal_id(),
                title=item.title,
                status="OPEN",
                priority=self._next_priority(doc),
                planned_minutes=item.planned_minutes,
            )
            key = self._goal_duplicate_key(goal)
            if key in existing_keys:
                continue
            doc.goals.append(goal)
            existing_keys.add(key)
            imported.append(goal)
        if imported:
            self.save(doc)
        return imported

    def find_goal(self, ref: str) -> DayGoal:
        doc = self.load()
        index = self._resolve_goal_index(doc, ref)
        return doc.goals[index]

    def set_goal_status(self, ref: str, status: str) -> DayGoal:
        doc = self.load()
        index = self._resolve_goal_index(doc, ref)
        old = doc.goals[index]
        new = replace(old, status=status)
        doc.goals[index] = new
        self.save(doc)
        return new

    def rename_goal(self, ref: str, title: str) -> DayGoal:
        doc = self.load()
        index = self._resolve_goal_index(doc, ref)
        old = doc.goals[index]
        new = replace(old, title=title.strip())
        doc.goals[index] = new
        self.save(doc)
        return new

    def sync_state(self, state: DayState) -> DayState:
        doc = self.load()
        open_minor = [
            self._format_minor(item)
            for item in doc.minor_activities
            if item.status in {"OPEN", "ACTIVE", "APPROVED"}
        ]
        active_goal_id = state.active_goal_id
        goal_ids = {goal.id for goal in doc.goals}
        if active_goal_id and active_goal_id not in goal_ids:
            active_goal_id = None
        if active_goal_id:
            active_goal = next((g for g in doc.goals if g.id == active_goal_id), None)
            if active_goal and active_goal.status in {"DONE", "DROPPED"}:
                active_goal_id = None
        return replace(state, approved_goals=doc.goals, allowed_minor=open_minor, active_goal_id=active_goal_id)

    def open_in_editor(self) -> None:
        self.ensure_exists()
        editor = os.environ.get("EDITOR")
        if editor:
            subprocess.run([editor, str(self.goals_file)], check=False)
            return
        subprocess.run(["xdg-open", str(self.goals_file)], check=False)

    def _resolve_goal_index(self, doc: GoalDocument, ref: str) -> int:
        normalized = ref.strip().lstrip("#")
        if normalized.isdigit():
            index = int(normalized) - 1
            if 0 <= index < len(doc.goals):
                return index
        for index, goal in enumerate(doc.goals):
            if goal.id == normalized:
                return index
        raise KeyError(f"Goal not found: {ref}")

    def _select_carryover(self, carry: list[CarryoverGoal], refs: str) -> list[CarryoverGoal]:
        normalized = refs.strip().lower()
        if normalized == "all":
            return carry
        if not normalized:
            raise KeyError("Usage: g in <number|all>, example: g in 1 or g in all")

        selected: list[CarryoverGoal] = []
        by_index = {item.index: item for item in carry}
        tokens = [token for token in re.split(r"[\s,]+", normalized) if token]
        for token in tokens:
            if not token.isdigit():
                raise KeyError(f"Carryover item must be a number or 'all': {token}")
            item = by_index.get(int(token))
            if item is None:
                raise KeyError(f"Carryover item not found: {token}")
            selected.append(item)
        return selected

    @staticmethod
    def _goal_duplicate_key(goal: DayGoal) -> tuple[str, int | None]:
        return (" ".join(goal.title.lower().split()), goal.planned_minutes)

    @staticmethod
    def _format_minor(item: MinorActivity) -> str:
        if item.time_limit_minutes:
            return f"{item.title} ({item.time_limit_minutes}m)"
        return item.title

    @staticmethod
    def _next_priority(doc: GoalDocument) -> int | None:
        priorities = [goal.priority for goal in doc.goals if getattr(goal, "priority", None) is not None]
        if not priorities:
            return 1
        return max(priorities) + 1
