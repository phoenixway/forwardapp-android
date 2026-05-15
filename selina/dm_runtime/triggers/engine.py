from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, time, timedelta
from typing import Iterable

from dm_runtime.config.settings import RuntimeSettings
from dm_runtime.domain.enums import DayStage, StateClass
from dm_runtime.domain.models import DayState


@dataclass(frozen=True)
class TriggerResult:
    id: str
    severity: str
    title: str
    message: str
    risk_flag: str
    protocol_id: str = "start_day"
    recommended_command: str = "session"


class TriggerEngine:
    """Evaluates automatic day-management trigger rules.

    This engine is pure: it only reads state/settings and returns trigger results.
    The app layer decides whether to persist alarms/events and show a session prompt.
    """

    def __init__(self, settings: RuntimeSettings):
        self.settings = settings

    def evaluate(self, state: DayState, now: datetime | None = None) -> list[TriggerResult]:
        now = now or datetime.now()
        results: list[TriggerResult] = []
        results.extend(self._no_plan_after_wake(state, now))
        results.extend(self._preparation_too_long(state, now))
        results.extend(self._active_goal_duration_exceeded(state, now))
        results.extend(self._task_timebox_exceeded(state, now))
        return results

    def _no_plan_after_wake(self, state: DayState, now: datetime) -> Iterable[TriggerResult]:
        wake_dt = self._wake_datetime(state, now)
        deadline = wake_dt + timedelta(minutes=self.settings.no_plan_after_wake_minutes)
        has_plan = bool([g for g in state.approved_goals if g.status not in {"DONE", "DROPPED"}])
        if now >= deadline and not has_plan and state.stage not in {DayStage.CLOSED, DayStage.FINALIZATION}:
            source = "wokeup" if state.user_woke_at else "configured wake time"
            yield TriggerResult(
                id=f"no_plan_after_wakeup:{state.date}",
                severity="ALARM",
                title="No day plan after wokeup deadline",
                message=(
                    f"No approved day goals {self.settings.no_plan_after_wake_minutes} minutes "
                    f"after {source} ({wake_dt.strftime('%H:%M')})."
                ),
                risk_flag="no_day_plan_after_wakeup_deadline",
                protocol_id="start_day",
                recommended_command="session",
            )

    def _preparation_too_long(self, state: DayState, now: datetime) -> Iterable[TriggerResult]:
        if state.stage != DayStage.PREPARATION or not state.preparation_started_at:
            return []
        started = self._parse_dt(state.preparation_started_at)
        if not started:
            return []
        elapsed = int((now - started).total_seconds() // 60)
        if elapsed > self.settings.max_preparation_minutes:
            return [TriggerResult(
                id=f"preparation_too_long:{state.date}",
                severity="ALARM",
                title="Day preparation is too long",
                message=(
                    f"Preparation has lasted {elapsed} minutes. Limit is "
                    f"{self.settings.max_preparation_minutes} minutes."
                ),
                risk_flag="preparation_too_long",
                protocol_id="discipline_violation",
                recommended_command="session",
            )]
        return []

    def _active_goal_duration_exceeded(self, state: DayState, now: datetime) -> Iterable[TriggerResult]:
        goal = state.active_goal()
        if not goal or not goal.planned_minutes or not state.active_goal_started_at:
            return []
        started = self._parse_dt(state.active_goal_started_at)
        if not started:
            return []
        elapsed = int((now - started).total_seconds() // 60)
        if elapsed > goal.planned_minutes:
            return [TriggerResult(
                id=f"goal_duration_exceeded:{state.date}:{goal.id}",
                severity="ALARM",
                title="Goal duration exceeded",
                message=(
                    f"Active goal {goal.id} exceeded planned duration: "
                    f"{elapsed}/{goal.planned_minutes} minutes. Discipline review required."
                ),
                risk_flag="goal_duration_exceeded",
                protocol_id="discipline_violation",
                recommended_command="session",
            )]
        return []

    def _task_timebox_exceeded(self, state: DayState, now: datetime) -> Iterable[TriggerResult]:
        if not state.timebox_started_at or not state.timebox_minutes or state.stage != DayStage.IMPLEMENTATION:
            return []
        started = self._parse_dt(state.timebox_started_at)
        if not started:
            return []
        elapsed = int((now - started).total_seconds() // 60)
        if elapsed > state.timebox_minutes:
            return [TriggerResult(
                id=f"task_timebox_exceeded:{state.date}:{state.timebox_started_at}",
                severity="ALARM",
                title="Task timebox exceeded",
                message=f"Current task exceeded timebox: {elapsed}/{state.timebox_minutes} minutes.",
                risk_flag="task_timebox_exceeded",
                protocol_id="discipline_violation",
                recommended_command="session",
            )]
        return []

    @staticmethod
    def _parse_dt(value: str) -> datetime | None:
        try:
            return datetime.fromisoformat(value)
        except ValueError:
            return None

    def _wake_datetime(self, state: DayState, now: datetime) -> datetime:
        if state.user_woke_at:
            parsed = self._parse_dt(state.user_woke_at)
            if parsed:
                return parsed
        return self._today_time(now, self.settings.wake_time)

    @staticmethod
    def _today_time(now: datetime, raw: str) -> datetime:
        try:
            hour, minute = [int(part) for part in raw.split(":", 1)]
            t = time(hour=hour, minute=minute)
        except Exception:
            t = time(hour=8, minute=0)
        return datetime.combine(now.date(), t)
