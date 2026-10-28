package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayFocusItem
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedRecurringTask
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata
import com.romankozak.forwardappmobile.shared.contracts.contexts.hasCanonicalPlanningTransport
import com.romankozak.forwardappmobile.shared.contracts.contexts.hasCanonicalDayPlanningExtensionTransport
import com.romankozak.forwardappmobile.shared.contracts.contexts.hasCanonicalTacticalPlanningExtensionTransport
import com.romankozak.forwardappmobile.shared.contracts.contexts.hasCanonicalStrategicPlanningExtensionTransport
import com.romankozak.forwardappmobile.shared.contracts.contexts.CURRENT_SHARED_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.shared.contracts.contexts.DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.shared.contracts.contexts.LEGACY_SHARED_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.shared.contracts.contexts.TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.shared.contracts.contexts.requireValidCanonicalPlanningTransport

class DesktopWorkspaceSnapshotMerger {
    fun merge(
        local: DesktopWorkspaceSnapshot,
        incoming: DesktopWorkspaceSnapshot,
    ): DesktopWorkspaceSnapshot {
        local.requireValidCanonicalPlanningTransport()
        incoming.requireValidCanonicalPlanningTransport()
        val planningSource = if (incoming.hasCanonicalPlanningTransport()) incoming else local
        val extensionSource =
            if (incoming.hasCanonicalDayPlanningExtensionTransport()) incoming else local
        val tacticalExtensionSource =
            if (incoming.hasCanonicalTacticalPlanningExtensionTransport()) incoming else local
        val strategicExtensionSource =
            if (incoming.hasCanonicalStrategicPlanningExtensionTransport()) incoming else local
        val planningFormatVersion =
            when {
                local.planningCommitmentRelations != null || incoming.planningCommitmentRelations != null ->
                    com.romankozak.forwardappmobile.shared.contracts.contexts.COMPOSITION_SHARED_PLANNING_FORMAT_VERSION
                planningSource.hasCanonicalPlanningTransport() &&
                    extensionSource.hasCanonicalDayPlanningExtensionTransport() &&
                    tacticalExtensionSource.hasCanonicalTacticalPlanningExtensionTransport() &&
                    strategicExtensionSource.hasCanonicalStrategicPlanningExtensionTransport() ->
                    CURRENT_SHARED_PLANNING_FORMAT_VERSION

                planningSource.hasCanonicalPlanningTransport() &&
                    extensionSource.hasCanonicalDayPlanningExtensionTransport() &&
                    tacticalExtensionSource.hasCanonicalTacticalPlanningExtensionTransport() ->
                    TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION

                planningSource.hasCanonicalPlanningTransport() &&
                    extensionSource.hasCanonicalDayPlanningExtensionTransport() ->
                    DAY_EXTENSION_SHARED_PLANNING_FORMAT_VERSION

                else -> planningSource.planningFormatVersion
            }

        return DesktopWorkspaceSnapshot(
            contexts = mergeById(local.contexts, incoming.contexts, SharedContextSummary::id, SharedContextSummary::sync),
            backlogItems = mergeById(local.backlogItems, incoming.backlogItems, SharedBacklogItem::id, SharedBacklogItem::sync),
            dayPlans = mergeById(local.dayPlans, incoming.dayPlans, SharedDayPlan::id, SharedDayPlan::sync),
            dayFocusItems = mergeById(local.dayFocusItems, incoming.dayFocusItems, SharedDayFocusItem::id, SharedDayFocusItem::sync),
            dayTasks = mergeById(local.dayTasks, incoming.dayTasks, SharedDayTask::id, SharedDayTask::sync),
            recurringTasks = mergeStaticById(local.recurringTasks, incoming.recurringTasks, SharedRecurringTask::id),
            planningFormatVersion = planningFormatVersion,
            planningScopes = planningSource.planningScopes,
            planningCommitments = planningSource.planningCommitments,
            planningCommitmentRelations = if (planningFormatVersion == 5) mergeComposition(
                local.planningCommitmentRelations.orEmpty(), incoming.planningCommitmentRelations.orEmpty()) else null,
            dayPlanningScopeExtensions =
                if (planningFormatVersion != LEGACY_SHARED_PLANNING_FORMAT_VERSION) {
                    extensionSource.dayPlanningScopeExtensions
                } else {
                    null
                },
            tacticalPlanningScopeExtensions =
                if (
                    planningFormatVersion in
                    setOf(TACTICAL_EXTENSION_SHARED_PLANNING_FORMAT_VERSION, CURRENT_SHARED_PLANNING_FORMAT_VERSION, 5)
                ) {
                    tacticalExtensionSource.tacticalPlanningScopeExtensions
                } else {
                    null
                },
            strategicPlanningScopeExtensions =
                if (planningFormatVersion in setOf(CURRENT_SHARED_PLANNING_FORMAT_VERSION, 5)) {
                    strategicExtensionSource.strategicPlanningScopeExtensions
                } else {
                    null
                },
        ).also(DesktopWorkspaceSnapshot::requireValidCanonicalPlanningTransport)
    }

    private fun mergeComposition(
        local: List<com.romankozak.forwardappmobile.shared.contracts.contexts.SharedPlanningCommitmentRelationSnapshot>,
        incoming: List<com.romankozak.forwardappmobile.shared.contracts.contexts.SharedPlanningCommitmentRelationSnapshot>,
    ): List<com.romankozak.forwardappmobile.shared.contracts.contexts.SharedPlanningCommitmentRelationSnapshot> {
        val rows = local.associateBy { it.id }.toMutableMap()
        incoming.forEach { next ->
            val old = rows[next.id]
            if (old != null) require(listOf(old.sourceCommitmentId, old.targetCommitmentId, old.relationType, old.createdAt) ==
                listOf(next.sourceCommitmentId, next.targetCommitmentId, next.relationType, next.createdAt)) { "Composition identity changed" }
            when {
                old == null || next.version > old.version -> rows[next.id] = next.copy(syncedAt = null)
                next.version < old.version -> Unit
                next.copy(syncedAt = null) == old.copy(syncedAt = null) -> Unit
                else -> error("Equal-version composition divergence")
            }
        }
        return rows.values.sortedBy { it.id }
    }

    private fun <T> mergeById(
        localItems: List<T>,
        incomingItems: List<T>,
        idSelector: (T) -> String,
        syncSelector: (T) -> SharedSyncMetadata,
    ): List<T> {
        val incomingById = incomingItems.associateBy(idSelector)
        val localById = localItems.associateBy(idSelector)
        val ids = linkedSetOf<String>().apply {
            addAll(localItems.map(idSelector))
            addAll(incomingItems.map(idSelector))
        }
        return ids.mapNotNull { id ->
            val local = localById[id]
            val incoming = incomingById[id]
            when {
                local == null -> incoming
                incoming == null -> local
                syncSelector(incoming).isNewerThan(syncSelector(local)) -> incoming
                else -> local
            }
        }
    }

    private fun <T> mergeStaticById(
        localItems: List<T>,
        incomingItems: List<T>,
        idSelector: (T) -> String,
    ): List<T> =
        (localItems + incomingItems)
            .associateBy(idSelector)
            .values
            .toList()
}

private fun SharedSyncMetadata.isNewerThan(other: SharedSyncMetadata): Boolean =
    updatedAt > other.updatedAt || (updatedAt == other.updatedAt && version > other.version)
