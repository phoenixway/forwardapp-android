from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any
from uuid import uuid4

from dm_runtime.domain.enums import DayStage, StateClass


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid4().hex[:8]}"


@dataclass
class DayGoal:
    id: str
    title: str
    status: str = "OPEN"
    priority: int | None = None
    planned_minutes: int | None = None

    @staticmethod
    def create(title: str, planned_minutes: int | None = None) -> "DayGoal":
        return DayGoal(
            id=new_id("goal"),
            title=title.strip(),
            status="OPEN",
            planned_minutes=planned_minutes,
        )


@dataclass
class DayState:
    date: str
    stage: DayStage = DayStage.NOT_STARTED
    state_class: StateClass = StateClass.IDLE
    approved_goals: list[DayGoal] = field(default_factory=list)
    allowed_minor: list[str] = field(default_factory=list)
    active_goal_id: str | None = None
    active_activity: str | None = None
    actual_activity: str | None = None
    active_protocol_id: str | None = None
    risk_flags: list[str] = field(default_factory=list)
    active_alarm_ids: list[str] = field(default_factory=list)
    user_woke_at: str | None = None
    day_started_at: str | None = None
    preparation_started_at: str | None = None
    implementation_started_at: str | None = None
    active_goal_started_at: str | None = None
    timebox_started_at: str | None = None
    timebox_minutes: int | None = None
    next_control_at: str | None = None

    def active_goal(self) -> DayGoal | None:
        if self.active_goal_id is None:
            return None
        return next((g for g in self.approved_goals if g.id == self.active_goal_id), None)


@dataclass
class RuntimeEvent:
    time: str
    type: str
    payload: dict[str, Any] = field(default_factory=dict)

    @staticmethod
    def create(event_type: str, payload: dict[str, Any] | None = None) -> "RuntimeEvent":
        return RuntimeEvent(
            time=datetime.now().isoformat(timespec="seconds"),
            type=event_type,
            payload=payload or {},
        )


@dataclass
class RuntimeDecision:
    new_state: DayState
    messages: list[str] = field(default_factory=list)
    events: list[RuntimeEvent] = field(default_factory=list)
    recommended_protocol_id: str | None = None
    recommended_next_command: str | None = None
    dashboard_required: bool = True
    open_file: str | None = None
