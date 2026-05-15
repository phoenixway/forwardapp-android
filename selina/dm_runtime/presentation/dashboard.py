from __future__ import annotations

from datetime import datetime

from rich.console import Console
from rich.markdown import Markdown
from rich.panel import Panel
from rich.table import Table
from rich.text import Text

from dm_runtime.domain.models import DayState, RuntimeDecision
from dm_runtime.goals.goal_store import CarryoverGoal
from dm_runtime.goals.markdown_codec import GoalDocument
from dm_runtime.protocols.protocol_models import Protocol
from dm_runtime.protocols.registry import ProtocolRegistry


class DashboardPresenter:
    def __init__(self, console: Console | None = None):
        self.console = console or Console()

    def show_decision(self, decision: RuntimeDecision, registry: ProtocolRegistry) -> None:
        for message in decision.messages:
            self.console.print(message)
        if decision.dashboard_required:
            self.show_state(decision.new_state, registry)
        if decision.recommended_protocol_id:
            protocol = registry.get(decision.recommended_protocol_id)
            if protocol:
                self.console.print(f"Recommended protocol: [bold]{protocol.id}[/bold] ({protocol.title})")
        if decision.recommended_next_command:
            self.console.print(f"Next command: [bold]{decision.recommended_next_command}[/bold]")

    def show_state(self, state: DayState, registry: ProtocolRegistry) -> None:
        table = Table.grid(expand=True)
        table.add_column(justify="left", ratio=1)
        table.add_column(justify="left", ratio=2)

        active_goal = state.active_goal()
        active_protocol = registry.get(state.active_protocol_id) if state.active_protocol_id else None

        table.add_row("Date", state.date)
        table.add_row("Woke up", self._format_dt(state.user_woke_at))
        table.add_row("Stage", f"[bold]{state.stage.value}[/bold]")
        table.add_row("State", f"[bold]{state.state_class.value}[/bold]")
        table.add_row("Active goal", active_goal.title if active_goal else "-")
        table.add_row("Active activity", state.active_activity or "-")
        table.add_row("Actual activity", state.actual_activity or "-")
        table.add_row("Protocol", active_protocol.id if active_protocol else (state.active_protocol_id or "-"))
        table.add_row("Goal timer", self._format_goal_timer(state))
        table.add_row("Timebox", self._format_timebox(state))
        table.add_row("Next control", self._format_dt(state.next_control_at))
        table.add_row("Risk flags", ", ".join(state.risk_flags) if state.risk_flags else "none")
        table.add_row("Active alarms", str(len(state.active_alarm_ids)) if state.active_alarm_ids else "none")

        self.console.print(Panel(table, title="Day Runtime", border_style="blue"))
        self._show_goals(state)
        self._show_allowed_minor(state)

    def _show_goals(self, state: DayState) -> None:
        table = Table(title="Approved Goals")
        table.add_column("#")
        table.add_column("ID")
        table.add_column("Title")
        table.add_column("Priority")
        table.add_column("Status")
        table.add_column("Plan")
        if not state.approved_goals:
            table.add_row("-", "-", "none", "-", "-")
        else:
            for index, goal in enumerate(state.approved_goals, 1):
                marker = "*" if goal.id == state.active_goal_id else str(index)
                priority = f"p{goal.priority}" if getattr(goal, "priority", None) else "-"
                planned = f"{goal.planned_minutes}m" if getattr(goal, "planned_minutes", None) else "-"
                table.add_row(marker, goal.id, goal.title, priority, goal.status, planned)
        self.console.print(table)

    def _show_allowed_minor(self, state: DayState) -> None:
        if not state.allowed_minor:
            return
        text = Text("\n".join(f"- {item}" for item in state.allowed_minor))
        self.console.print(Panel(text, title="Allowed Minor Activities", border_style="green"))


    def show_goal_document(self, doc: GoalDocument, active_goal_id: str | None = None) -> None:
        goals = Table(title="Day Goals")
        goals.add_column("#")
        goals.add_column("ID")
        goals.add_column("Priority")
        goals.add_column("Status")
        goals.add_column("Title")
        goals.add_column("Plan")
        if not doc.goals:
            goals.add_row("-", "-", "-", "-", "none", "-")
        else:
            for index, goal in enumerate(doc.goals, 1):
                marker = "*" if goal.id == active_goal_id else str(index)
                priority = f"p{goal.priority}" if getattr(goal, "priority", None) else "-"
                planned = f"{goal.planned_minutes}m" if getattr(goal, "planned_minutes", None) else "-"
                goals.add_row(marker, goal.id, priority, goal.status, goal.title, planned)
        self.console.print(goals)

        if doc.minor_activities:
            minor = Table(title="Allowed Minor Activities")
            minor.add_column("#")
            minor.add_column("ID")
            minor.add_column("Limit")
            minor.add_column("Status")
            minor.add_column("Title")
            for index, item in enumerate(doc.minor_activities, 1):
                limit = f"{item.time_limit_minutes}m" if item.time_limit_minutes else "-"
                minor.add_row(str(index), item.id, limit, item.status, item.title)
            self.console.print(minor)

        if doc.parking_lot:
            parking = Table(title="Parking Lot")
            parking.add_column("#")
            parking.add_column("Status")
            parking.add_column("Title")
            for index, item in enumerate(doc.parking_lot, 1):
                parking.add_row(str(index), item.status, item.title)
            self.console.print(parking)



    def show_carryover_goals(self, carryover: list[CarryoverGoal]) -> None:
        table = Table(title="Carryover Goals")
        table.add_column("#")
        table.add_column("Date")
        table.add_column("Old ID")
        table.add_column("Status")
        table.add_column("Plan")
        table.add_column("Title")
        if not carryover:
            table.add_row("-", "-", "-", "-", "-", "none")
        else:
            for item in carryover:
                planned = f"{item.planned_minutes}m" if item.planned_minutes else "-"
                table.add_row(
                    str(item.index),
                    item.source_date,
                    item.source_goal_id,
                    item.status,
                    planned,
                    item.title,
                )
        self.console.print(table)
        if carryover:
            self.console.print("Import with: [bold]g in <number>[/bold], [bold]g in 1 2 3[/bold], or [bold]g in all[/bold]")

    def show_protocol(self, protocol: Protocol) -> None:
        if protocol.body_markdown:
            body = protocol.body_markdown
            if protocol.recommended_next_command:
                body += f"\n\n**Recommended next command:** `{protocol.recommended_next_command}`"
            if protocol.source_path:
                body += f"\n\n_Source: `{protocol.source_path}`_"
            self.console.print(Panel(Markdown(body), title=f"Protocol: {protocol.id}", border_style="magenta"))
            return

        body = Text()
        body.append(protocol.description.strip() + "\n\n")
        if protocol.steps:
            body.append("Steps:\n", style="bold")
            for index, step in enumerate(protocol.steps, 1):
                body.append(f"{index}. {step.title}\n", style="bold")
                if step.instruction:
                    body.append(f"   instruction: {step.instruction}\n")
                if step.prompt:
                    body.append(f"   prompt: {step.prompt}\n")
        if protocol.recommended_next_command:
            body.append(f"\nRecommended next command: {protocol.recommended_next_command}\n", style="bold")
        if protocol.source_path:
            body.append(f"\nSource: {protocol.source_path}\n")
        self.console.print(Panel(body, title=f"Protocol: {protocol.id}", border_style="magenta"))

    def show_protocols(self, protocols: list[Protocol]) -> None:
        table = Table(title="Available Protocols")
        table.add_column("ID")
        table.add_column("Kind")
        table.add_column("Title")
        for protocol in protocols:
            table.add_row(protocol.id, protocol.kind, protocol.title)
        self.console.print(table)


    def _format_goal_timer(self, state: DayState) -> str:
        goal = state.active_goal()
        if not goal or not state.active_goal_started_at:
            return "-"
        try:
            started = datetime.fromisoformat(state.active_goal_started_at)
            elapsed = int((datetime.now() - started).total_seconds() // 60)
            if getattr(goal, "planned_minutes", None):
                return f"{elapsed}/{goal.planned_minutes} min"
            return f"{elapsed} min"
        except ValueError:
            return "?"

    def _format_timebox(self, state: DayState) -> str:
        if not state.timebox_started_at or not state.timebox_minutes:
            return "-"
        try:
            started = datetime.fromisoformat(state.timebox_started_at)
            elapsed = int((datetime.now() - started).total_seconds() // 60)
            return f"{elapsed}/{state.timebox_minutes} min"
        except ValueError:
            return f"?/ {state.timebox_minutes} min"

    def _format_dt(self, value: str | None) -> str:
        if not value:
            return "-"
        try:
            return datetime.fromisoformat(value).strftime("%H:%M")
        except ValueError:
            return value
