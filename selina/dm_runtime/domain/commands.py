from __future__ import annotations

from dataclasses import dataclass

from dm_runtime.domain.enums import StateClass


@dataclass(frozen=True)
class StartDay:
    pass


@dataclass(frozen=True)
class ApproveGoal:
    title: str


@dataclass(frozen=True)
class AllowMinorActivity:
    title: str


@dataclass(frozen=True)
class StartImplementation:
    pass


@dataclass(frozen=True)
class StartTask:
    title: str
    timebox_minutes: int | None = None


@dataclass(frozen=True)
class ControlCheck:
    actual_activity: str
    classification: StateClass


@dataclass(frozen=True)
class DriftDetected:
    actual_activity: str | None = None


@dataclass(frozen=True)
class FinalizeDay:
    completed: str
    open_items: str
    lesson: str


@dataclass(frozen=True)
class ResetDay:
    pass
