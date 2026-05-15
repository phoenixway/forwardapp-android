from __future__ import annotations

from dataclasses import asdict
from typing import Any

from dm_runtime.domain.enums import DayStage, StateClass
from dm_runtime.domain.models import DayGoal, DayState


def state_to_dict(state: DayState) -> dict[str, Any]:
    data = asdict(state)
    data["stage"] = state.stage.value
    data["state_class"] = state.state_class.value
    return data


def state_from_dict(data: dict[str, Any]) -> DayState:
    goals = [DayGoal(**_normalize_goal(item)) for item in data.get("approved_goals", [])]
    return DayState(
        date=data["date"],
        stage=DayStage(data.get("stage", DayStage.NOT_STARTED.value)),
        state_class=StateClass(data.get("state_class", StateClass.IDLE.value)),
        approved_goals=goals,
        allowed_minor=list(data.get("allowed_minor", [])),
        active_goal_id=data.get("active_goal_id"),
        active_activity=data.get("active_activity"),
        actual_activity=data.get("actual_activity"),
        active_protocol_id=data.get("active_protocol_id"),
        risk_flags=list(data.get("risk_flags", [])),
        active_alarm_ids=list(data.get("active_alarm_ids", [])),
        user_woke_at=data.get("user_woke_at"),
        day_started_at=data.get("day_started_at"),
        preparation_started_at=data.get("preparation_started_at"),
        implementation_started_at=data.get("implementation_started_at"),
        active_goal_started_at=data.get("active_goal_started_at"),
        timebox_started_at=data.get("timebox_started_at"),
        timebox_minutes=data.get("timebox_minutes"),
        next_control_at=data.get("next_control_at"),
    )


def _normalize_goal(item: dict[str, Any]) -> dict[str, Any]:
    data = dict(item)
    data.setdefault("planned_minutes", None)
    return data
