from __future__ import annotations

from dataclasses import replace
from datetime import datetime, timedelta
from typing import Any

from dm_runtime.domain.commands import (
    AllowMinorActivity,
    ApproveGoal,
    ControlCheck,
    DriftDetected,
    FinalizeDay,
    ResetDay,
    StartDay,
    StartImplementation,
    StartTask,
)
from dm_runtime.domain.enums import DayStage, EventType, StateClass
from dm_runtime.domain.models import DayGoal, DayState, RuntimeDecision, RuntimeEvent
from dm_runtime.engine.router import ProtocolRouter


class DayRuntime:
    def __init__(self, router: ProtocolRouter):
        self.router = router

    def handle(self, state: DayState, command: Any) -> RuntimeDecision:
        if isinstance(command, StartDay):
            return self._start_day(state)
        if isinstance(command, ApproveGoal):
            return self._approve_goal(state, command)
        if isinstance(command, AllowMinorActivity):
            return self._allow_minor(state, command)
        if isinstance(command, StartImplementation):
            return self._start_implementation(state)
        if isinstance(command, StartTask):
            return self._start_task(state, command)
        if isinstance(command, ControlCheck):
            return self._control_check(state, command)
        if isinstance(command, DriftDetected):
            return self._drift_detected(state, command)
        if isinstance(command, FinalizeDay):
            return self._finalize_day(state, command)
        if isinstance(command, ResetDay):
            return self._reset_day(state)
        return RuntimeDecision(state, messages=[f"Unsupported command: {type(command).__name__}"])

    def _event(self, event_type: EventType, payload: dict[str, Any] | None = None) -> RuntimeEvent:
        return RuntimeEvent.create(event_type.value, payload)

    def _with_route(self, state: DayState, messages: list[str], events: list[RuntimeEvent]) -> RuntimeDecision:
        protocol_id = self.router.route(state)
        next_command = None
        if protocol_id:
            protocol = self.router.registry.get(protocol_id)
            next_command = protocol.recommended_next_command if protocol else None
        return RuntimeDecision(
            new_state=state,
            messages=messages,
            events=events,
            recommended_protocol_id=protocol_id,
            recommended_next_command=next_command,
        )

    def _start_day(self, state: DayState) -> RuntimeDecision:
        now = datetime.now().isoformat(timespec="seconds")
        new_state = replace(
            state,
            stage=DayStage.PREPARATION,
            state_class=StateClass.IDLE,
            active_protocol_id="start_day",
            risk_flags=[],
            day_started_at=state.day_started_at or now,
            preparation_started_at=state.preparation_started_at or now,
        )
        return self._with_route(
            new_state,
            ["Day preparation started.", "Next: add 1-3 approved goals with: g add <text> or edit with: g edit"],
            [self._event(EventType.DAY_STARTED)],
        )

    def _approve_goal(self, state: DayState, command: ApproveGoal) -> RuntimeDecision:
        title = command.title.strip()
        if not title:
            return RuntimeDecision(state, messages=["Goal text is empty."])
        goal = DayGoal.create(title)
        new_state = replace(state, approved_goals=[*state.approved_goals, goal])
        return self._with_route(
            new_state,
            [f"Approved goal: {goal.title}"],
            [self._event(EventType.GOAL_APPROVED, {"goal_id": goal.id, "title": goal.title})],
        )

    def _allow_minor(self, state: DayState, command: AllowMinorActivity) -> RuntimeDecision:
        title = command.title.strip()
        if not title:
            return RuntimeDecision(state, messages=["Minor activity text is empty."])
        new_state = replace(state, allowed_minor=[*state.allowed_minor, title])
        return self._with_route(
            new_state,
            [f"Allowed minor activity: {title}"],
            [self._event(EventType.MINOR_ACTIVITY_ALLOWED, {"title": title})],
        )

    def _start_implementation(self, state: DayState) -> RuntimeDecision:
        if state.stage == DayStage.NOT_STARTED:
            return RuntimeDecision(state, messages=["Day is not started. Run: start day"])
        open_goals = [g for g in state.approved_goals if g.status not in {"DONE", "DROPPED"}]
        if not open_goals:
            return RuntimeDecision(state, messages=["No open approved goals. Add one with: g add <text> or edit with: g edit"])
        active_goal_id = state.active_goal_id or open_goals[0].id
        now = datetime.now().isoformat(timespec="seconds")
        new_state = replace(
            state,
            stage=DayStage.IMPLEMENTATION,
            state_class=StateClass.IDLE,
            active_goal_id=active_goal_id,
            active_protocol_id="implementation",
            implementation_started_at=state.implementation_started_at or now,
            active_goal_started_at=state.active_goal_started_at or now,
        )
        return self._with_route(
            new_state,
            ["Implementation stage started.", "Next: start task <text>"],
            [self._event(EventType.IMPLEMENTATION_STARTED, {"active_goal_id": active_goal_id})],
        )

    def _start_task(self, state: DayState, command: StartTask) -> RuntimeDecision:
        title = command.title.strip()
        if state.stage != DayStage.IMPLEMENTATION:
            return RuntimeDecision(state, messages=["Not in implementation stage. Run: start impl"])
        if not title:
            return RuntimeDecision(state, messages=["Task text is empty."])
        minutes = command.timebox_minutes or state.timebox_minutes or 45
        now = datetime.now()
        new_state = replace(
            state,
            active_activity=title,
            actual_activity=title,
            state_class=StateClass.ON_TARGET,
            active_protocol_id="implementation",
            timebox_started_at=now.isoformat(timespec="seconds"),
            timebox_minutes=minutes,
            next_control_at=(now + timedelta(minutes=minutes)).isoformat(timespec="seconds"),
            risk_flags=[],
            active_goal_started_at=state.active_goal_started_at or now.isoformat(timespec="seconds"),
        )
        return self._with_route(
            new_state,
            [f"Started task: {title}", f"Timebox: {minutes} min"],
            [self._event(EventType.TASK_STARTED, {"title": title, "timebox_minutes": minutes})],
        )

    def _control_check(self, state: DayState, command: ControlCheck) -> RuntimeDecision:
        risk_flags = list(state.risk_flags)
        protocol_id = state.active_protocol_id
        message = "State checked."
        if command.classification == StateClass.DRIFT:
            protocol_id = "stop_unwanted_activity"
            if "unapproved_activity" not in risk_flags:
                risk_flags.append("unapproved_activity")
            message = "DRIFT detected during control check."
        elif command.classification == StateClass.MINOR_USEFUL:
            protocol_id = "minor_useful_activity"
            message = "Minor useful activity detected. Keep it timeboxed."
        elif command.classification == StateClass.RECOVERY:
            protocol_id = "recovery"
            message = "Recovery/maintenance state detected. Make it deliberate and timeboxed."
        elif command.classification == StateClass.ON_TARGET:
            protocol_id = "implementation"
            risk_flags = [flag for flag in risk_flags if flag != "unapproved_activity"]
            message = "On target. Continue."

        new_state = replace(
            state,
            actual_activity=command.actual_activity.strip(),
            state_class=command.classification,
            active_protocol_id=protocol_id,
            risk_flags=risk_flags,
        )
        return self._with_route(
            new_state,
            [message],
            [self._event(EventType.CONTROL_CHECK, {
                "actual_activity": command.actual_activity.strip(),
                "classification": command.classification.value,
            })],
        )

    def _drift_detected(self, state: DayState, command: DriftDetected) -> RuntimeDecision:
        actual = command.actual_activity.strip() if command.actual_activity else state.actual_activity
        risk_flags = list(state.risk_flags)
        if "unapproved_activity" not in risk_flags:
            risk_flags.append("unapproved_activity")
        new_state = replace(
            state,
            actual_activity=actual,
            state_class=StateClass.DRIFT,
            active_protocol_id="stop_unwanted_activity",
            risk_flags=risk_flags,
        )
        return self._with_route(
            new_state,
            ["DRIFT detected.", "Protocol selected: stop_unwanted_activity"],
            [self._event(EventType.DRIFT_DETECTED, {"actual_activity": actual})],
        )

    def _finalize_day(self, state: DayState, command: FinalizeDay) -> RuntimeDecision:
        finalizing = replace(state, stage=DayStage.FINALIZATION, active_protocol_id="day_finalization")
        closed = replace(
            finalizing,
            stage=DayStage.CLOSED,
            state_class=StateClass.IDLE,
            active_goal_id=None,
            active_activity=None,
            actual_activity=None,
            active_protocol_id=None,
            risk_flags=[],
            active_alarm_ids=[],
            next_control_at=None,
        )
        return RuntimeDecision(
            new_state=closed,
            messages=["Day closed."],
            events=[
                self._event(EventType.DAY_FINALIZATION_STARTED),
                self._event(EventType.DAY_FINALIZED, {
                    "completed": command.completed,
                    "open_items": command.open_items,
                    "lesson": command.lesson,
                }),
            ],
            recommended_protocol_id="day_finalization",
            dashboard_required=True,
        )

    def _reset_day(self, state: DayState) -> RuntimeDecision:
        fresh = DayState(date=state.date)
        return RuntimeDecision(
            new_state=fresh,
            messages=["Current day state reset."],
            events=[self._event(EventType.STATE_RESET)],
        )
