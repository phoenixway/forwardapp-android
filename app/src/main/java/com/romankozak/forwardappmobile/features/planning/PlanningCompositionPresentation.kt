package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

/** Presentation over an already loaded canonical frame. Repository validation remains authoritative. */
internal fun PlanningCommitmentCompositionProjection.relationCandidates(
    current: PlanningCommitmentId, addingParent: Boolean,
): List<PlanningParticipantSummary> {
    fun reachable(start: PlanningCommitmentId, upward: Boolean): Set<PlanningCommitmentId> {
        val visited = mutableSetOf<PlanningCommitmentId>()
        val pending = ArrayDeque<PlanningCommitmentId>().apply { add(start) }
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            if (!visited.add(next)) continue
            val edges = if (upward) outgoing(next) else incoming(next)
            edges.forEach { pending.add(it.participant.commitment.id) }
        }
        return visited
    }
    val blocked = reachable(current, upward = !addingParent) +
        (if (addingParent) outgoing(current) else incoming(current)).map { it.participant.commitment.id }
    return participants.filter { it.commitment.id !in blocked }
}

internal fun PlanningParticipantSummary.planningDestination(): NavTarget {
    val task = dayTaskId ?: commitment.provenance.sourceId.takeIf { commitment.provenance.sourceType == "DAY_TASK" }
    return if (task != null) NavTarget.EditTask(task) else NavTarget.PlanningParticipation(commitment.id.value)
}

internal val PlanningParticipantSummary.statusLabel: String get() = when (commitment.status.name) {
    "COMPLETED" -> "Завершено"
    "DROPPED" -> "Скасовано"
    "PAUSED" -> "Призупинено"
    else -> ""
}

/** Path-local guards preserve branching DAG paths, including a shared ancestor on different paths. */
internal data class PlanningTraceRow(val key: String, val participant: PlanningParticipantSummary, val depth: Int, val hasMore: Boolean)

internal fun PlanningCommitmentCompositionProjection.trace(
    root: PlanningCommitmentId, upward: Boolean, expanded: Set<String> = emptySet(), limit: Int = 200,
): List<PlanningTraceRow> {
    val rows = mutableListOf<PlanningTraceRow>()
    fun visit(id: PlanningCommitmentId, path: Set<PlanningCommitmentId>, key: String, depth: Int) {
        if (rows.size >= limit || depth > 12) return
        val edges = if (upward) outgoing(id) else incoming(id)
        edges.forEach { edge ->
            val next = edge.participant.commitment.id
            if (next in path || rows.size >= limit) return@forEach
            val rowKey = "$key/${edge.relation.id.value}"
            val children = if (upward) outgoing(next) else incoming(next)
            val folded = depth >= 2 && rowKey !in expanded && children.isNotEmpty()
            rows += PlanningTraceRow(rowKey, edge.participant, depth, folded)
            if (!folded) visit(next, path + next, rowKey, depth + 1)
        }
    }
    visit(root, setOf(root), root.value, 0)
    return rows
}
