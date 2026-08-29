package com.romankozak.forwardappmobile.core.data.models.sync

/**
 * Local-only selection of row versions chosen for an outbound sync attempt.
 *
 * This is not a wire model and contains no Room entities.
 * SnapshotBundle remains the synchronization payload contract.
 */
data class LocalSyncVersion(
    val id: String,
    val version: Long,
)

data class LocalSyncCrossRefVersion(
    val contextId: String,
    val attachmentId: String,
    val version: Long,
)

data class LocalSyncSelection(
    val contexts: List<LocalSyncVersion> = emptyList(),
    val goals: List<LocalSyncVersion> = emptyList(),
    val backlogItems: List<LocalSyncVersion> = emptyList(),
    val backlogOrders: List<LocalSyncVersion> = emptyList(),
    val notes: List<LocalSyncVersion> = emptyList(),
    val documents: List<LocalSyncVersion> = emptyList(),
    val musicNotes: List<LocalSyncVersion> = emptyList(),
    val checklists: List<LocalSyncVersion> = emptyList(),
    val checklistItems: List<LocalSyncVersion> = emptyList(),
    val activityRecords: List<LocalSyncVersion> = emptyList(),
    val linkItemEntities: List<LocalSyncVersion> = emptyList(),
    val directionItems: List<LocalSyncVersion> = emptyList(),
    val inbox: List<LocalSyncVersion> = emptyList(),
    val logs: List<LocalSyncVersion> = emptyList(),
    val scripts: List<LocalSyncVersion> = emptyList(),
    val attachments: List<LocalSyncVersion> = emptyList(),
    val crossRefs: List<LocalSyncCrossRefVersion> = emptyList(),
    val dayPlans: List<LocalSyncVersion> = emptyList(),
    val dayFocusItems: List<LocalSyncVersion> = emptyList(),
    val dayTasks: List<LocalSyncVersion> = emptyList(),
    val tacticalMissions: List<LocalSyncVersion> = emptyList(),
    val tacticalIterations: List<LocalSyncVersion> = emptyList(),
    val missionStreams: List<LocalSyncVersion> = emptyList(),
    val tacticalActivitySlots: List<LocalSyncVersion> = emptyList(),
    val arcQuests: List<LocalSyncVersion> = emptyList(),
) {
    fun isEmpty(): Boolean =
        contexts.isEmpty() &&
            goals.isEmpty() &&
            backlogItems.isEmpty() &&
            backlogOrders.isEmpty() &&
            notes.isEmpty() &&
            documents.isEmpty() &&
            musicNotes.isEmpty() &&
            checklists.isEmpty() &&
            checklistItems.isEmpty() &&
            activityRecords.isEmpty() &&
            linkItemEntities.isEmpty() &&
            directionItems.isEmpty() &&
            inbox.isEmpty() &&
            logs.isEmpty() &&
            scripts.isEmpty() &&
            attachments.isEmpty() &&
            crossRefs.isEmpty() &&
            dayPlans.isEmpty() &&
            dayFocusItems.isEmpty() &&
            dayTasks.isEmpty() &&
            tacticalMissions.isEmpty() &&
            tacticalIterations.isEmpty() &&
            missionStreams.isEmpty() &&
            tacticalActivitySlots.isEmpty() &&
            arcQuests.isEmpty()
}
