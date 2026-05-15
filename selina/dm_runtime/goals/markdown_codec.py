from __future__ import annotations

import re
from dataclasses import dataclass, field

from dm_runtime.domain.models import DayGoal


GOAL_LINE_RE = re.compile(
    r"^- \[(?P<box>[ xX-])\]\s+#(?P<id>[A-Za-z]+\d+)"
    r"(?:\s+p(?P<priority>\d+))?"
    r"(?:\s+(?P<limit>\d+)m)?"
    r"\s+(?P<title>.+?)\s*$"
)

SECTION_RE = re.compile(r"^##\s+(?P<title>.+?)\s*$")

STATUS_BY_BOX = {
    " ": "OPEN",
    "x": "DONE",
    "X": "DONE",
    "-": "DROPPED",
}

BOX_BY_STATUS = {
    "OPEN": " ",
    "APPROVED": " ",
    "ACTIVE": " ",
    "DONE": "x",
    "DROPPED": "-",
}


@dataclass
class MinorActivity:
    id: str
    title: str
    status: str = "OPEN"
    time_limit_minutes: int | None = None


@dataclass
class ParkingItem:
    title: str
    status: str = "OPEN"


@dataclass
class GoalDocument:
    goals: list[DayGoal] = field(default_factory=list)
    minor_activities: list[MinorActivity] = field(default_factory=list)
    parking_lot: list[ParkingItem] = field(default_factory=list)

    def next_goal_id(self) -> str:
        return _next_id("g", [goal.id for goal in self.goals])

    def next_minor_id(self) -> str:
        return _next_id("m", [item.id for item in self.minor_activities])


def default_goals_markdown() -> str:
    return """# Day Goals\n\n## Approved Goals\n\n<!-- Add goals with: g add <text> or g add <text> ::90 -->\n\n## Allowed Minor Activities\n\n<!-- Add allowed minor activity with: m add <text> [::10] -->\n\n## Parking Lot\n\n<!-- Capture non-approved ideas with: inbox <text> -->\n"""


def parse_goals_markdown(text: str) -> GoalDocument:
    doc = GoalDocument()
    section = ""
    for raw_line in text.splitlines():
        line = raw_line.strip()
        section_match = SECTION_RE.match(line)
        if section_match:
            section = section_match.group("title").strip().lower()
            continue

        match = GOAL_LINE_RE.match(line)
        if match:
            status = STATUS_BY_BOX[match.group("box")]
            item_id = match.group("id")
            title = match.group("title").strip()
            priority_raw = match.group("priority")
            limit_raw = match.group("limit")

            if section == "approved goals":
                doc.goals.append(
                    DayGoal(
                        id=item_id,
                        title=title,
                        status=status,
                        priority=int(priority_raw) if priority_raw else None,
                        planned_minutes=int(limit_raw) if limit_raw else None,
                    )
                )
            elif section == "allowed minor activities":
                doc.minor_activities.append(
                    MinorActivity(
                        id=item_id,
                        title=title,
                        status=status,
                        time_limit_minutes=int(limit_raw) if limit_raw else None,
                    )
                )
            elif section == "parking lot":
                doc.parking_lot.append(ParkingItem(title=title, status=status))
            continue

        # Parking lot may also contain loose checklist items without ids.
        if section == "parking lot" and line.startswith("- ["):
            box = line[3:4]
            title = line[6:].strip() if len(line) > 6 else ""
            if title:
                doc.parking_lot.append(ParkingItem(title=title, status=STATUS_BY_BOX.get(box, "OPEN")))

    return doc


def render_goals_markdown(doc: GoalDocument) -> str:
    lines: list[str] = ["# Day Goals", "", "## Approved Goals", ""]
    if doc.goals:
        for goal in doc.goals:
            box = BOX_BY_STATUS.get(goal.status, " ")
            priority = f" p{goal.priority}" if getattr(goal, "priority", None) else ""
            planned = f" {goal.planned_minutes}m" if getattr(goal, "planned_minutes", None) else ""
            lines.append(f"- [{box}] #{goal.id}{priority}{planned} {goal.title}")
    else:
        lines.append("<!-- Add goals with: g add <text> or g add <text> ::90 -->")

    lines += ["", "## Allowed Minor Activities", ""]
    if doc.minor_activities:
        for item in doc.minor_activities:
            box = BOX_BY_STATUS.get(item.status, " ")
            limit = f" {item.time_limit_minutes}m" if item.time_limit_minutes else ""
            lines.append(f"- [{box}] #{item.id}{limit} {item.title}")
    else:
        lines.append("<!-- Add allowed minor activity with: minor-allow <text> -->")

    lines += ["", "## Parking Lot", ""]
    if doc.parking_lot:
        for item in doc.parking_lot:
            box = BOX_BY_STATUS.get(item.status, " ")
            lines.append(f"- [{box}] {item.title}")
    else:
        lines.append("<!-- Capture non-approved ideas with: inbox <text> -->")

    lines.append("")
    return "\n".join(lines)


def _next_id(prefix: str, ids: list[str]) -> str:
    max_num = 0
    for item_id in ids:
        if not item_id.startswith(prefix):
            continue
        suffix = item_id[len(prefix):]
        if suffix.isdigit():
            max_num = max(max_num, int(suffix))
    return f"{prefix}{max_num + 1}"
