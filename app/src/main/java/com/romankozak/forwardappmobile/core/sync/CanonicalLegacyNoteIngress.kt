package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.database.AppDatabase

/**
 * Restore replaces Note rows exactly; normal merge respects Note ID and version.
 * Historical contextId is compatibility evidence, never merge content or ownership.
 */
internal suspend fun applyCanonicalLegacyNotes(
    dao: LegacyNoteDao,
    incoming: List<LegacyNoteEntity>,
    restoreExact: Boolean,
) {
    require(incoming.map { it.id }.distinct().size == incoming.size) {
        "Canonical Note ingress has duplicate Note IDs"
    }

    incoming.forEach { note ->
        require(note.id.isNotBlank()) { "Canonical Note ID must not be blank" }
        require(note.version >= 0L) { "Canonical Note version must not be negative" }
    }

    if (restoreExact) {
        dao.insertAll(incoming)
        return
    }

    incoming.forEach { note ->
        val local = dao.getNoteById(note.id)

        when {
            local == null || note.version > local.version -> dao.insert(note)
            note.version < local.version -> Unit
            else -> require(local.sameAuthoritativeNoteContent(note)) {
                "Canonical Note same-version content conflict: ${note.id}"
            }
        }

        // Compatible same-version replay retains local contextId and syncedAt.
    }
}

private fun LegacyNoteEntity.sameAuthoritativeNoteContent(
    other: LegacyNoteEntity,
): Boolean =
    title == other.title &&
        content == other.content &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted

/**
 * Live Workspace placements require live Note targets.
 * Standalone Notes are valid without placements or Context.
 */
internal suspend fun requireCanonicalNotePlacementClosure(
    database: AppDatabase,
) {
    val liveNoteIds =
        database.legacyNoteDao().getAllRaw()
            .asSequence()
            .filterNot { it.isDeleted }
            .mapTo(hashSetOf()) { it.id }

    val orphanIds =
        database.workspaceBacklogEntryDao().getAll()
            .asSequence()
            .filter {
                !it.isDeleted &&
                    it.targetKind == "LEGACY_NOTE" &&
                    it.targetId !in liveNoteIds
            }
            .map { it.id }
            .toList()

    require(orphanIds.isEmpty()) {
        "Canonical BACKLOG has live placements without a live Note: ${orphanIds.joinToString()}"
    }
}
