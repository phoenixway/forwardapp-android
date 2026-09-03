package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind

/** Typed target identities required by live canonical BACKLOG placements. */
internal data class CanonicalBacklogTargetDependencies(
    val orientationIds: Set<String> = emptySet(),
    val workspaceIds: Set<String> = emptySet(),
    val linkItemIds: Set<String> = emptySet(),
    val legacyNoteIds: Set<String> = emptySet(),
    val documentIds: Set<String> = emptySet(),
    val checklistIds: Set<String> = emptySet(),
    val musicNoteIds: Set<String> = emptySet(),
)

internal fun Iterable<WorkspaceBacklogEntrySnapshot>.canonicalBacklogTargetDependencies():
    CanonicalBacklogTargetDependencies {
    val orientationIds = linkedSetOf<String>()
    val workspaceIds = linkedSetOf<String>()
    val linkItemIds = linkedSetOf<String>()
    val legacyNoteIds = linkedSetOf<String>()
    val documentIds = linkedSetOf<String>()
    val checklistIds = linkedSetOf<String>()
    val musicNoteIds = linkedSetOf<String>()

    filterNot { it.isDeleted }.forEach { entry ->
        when (
            runCatching { WorkspaceBacklogTargetKind.valueOf(entry.targetKind) }
                .getOrElse { error("Canonical BACKLOG target kind is unsupported: ${entry.targetKind}") }
        ) {
            WorkspaceBacklogTargetKind.ORIENTATION -> orientationIds += entry.targetId
            WorkspaceBacklogTargetKind.WORKSPACE -> workspaceIds += entry.targetId
            WorkspaceBacklogTargetKind.LINK_ITEM -> linkItemIds += entry.targetId
            WorkspaceBacklogTargetKind.LEGACY_NOTE -> legacyNoteIds += entry.targetId
            WorkspaceBacklogTargetKind.NOTE_DOCUMENT,
            -> documentIds += entry.targetId
            WorkspaceBacklogTargetKind.CHECKLIST -> checklistIds += entry.targetId
            WorkspaceBacklogTargetKind.MUSIC_NOTE -> musicNoteIds += entry.targetId
        }
    }

    return CanonicalBacklogTargetDependencies(
        orientationIds = orientationIds,
        workspaceIds = workspaceIds,
        linkItemIds = linkItemIds,
        legacyNoteIds = legacyNoteIds,
        documentIds = documentIds,
        checklistIds = checklistIds,
        musicNoteIds = musicNoteIds,
    )
}
