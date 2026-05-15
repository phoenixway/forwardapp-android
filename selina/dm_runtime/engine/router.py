from __future__ import annotations

from dm_runtime.domain.enums import DayStage, StateClass
from dm_runtime.domain.models import DayState
from dm_runtime.protocols.registry import ProtocolRegistry


class ProtocolRouter:
    def __init__(self, registry: ProtocolRegistry):
        self.registry = registry

    def route(self, state: DayState) -> str | None:
        if state.stage == DayStage.NOT_STARTED:
            return "start_day"
        if state.stage == DayStage.PREPARATION:
            return "start_day" if not state.approved_goals else "choose_day_target"
        if state.state_class == StateClass.DRIFT:
            return "stop_unwanted_activity"
        if state.state_class == StateClass.RECOVERY:
            return "recovery"
        if state.stage == DayStage.IMPLEMENTATION and not state.active_goal_id:
            return "choose_day_target"
        if state.stage == DayStage.IMPLEMENTATION:
            return state.active_protocol_id or "implementation"
        if state.stage == DayStage.FINALIZATION:
            return "day_finalization"
        return state.active_protocol_id
