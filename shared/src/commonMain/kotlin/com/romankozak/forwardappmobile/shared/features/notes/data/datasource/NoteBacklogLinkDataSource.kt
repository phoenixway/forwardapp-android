package com.romankozak.forwardappmobile.shared.features.notes.data.datasource

/**
 * Abstracts access to the project backlog (`list_items`) for shared note logic.
 */
interface NoteBacklogLinkDataSource {
    suspend fun insertLink(entry: NoteBacklogLink)
    suspend fun deleteLinkByEntityId(entityId: String)
}

/**
 * Minimal representation of a backlog link row.
 */
data class NoteBacklogLink(
    val id: String,
    val projectId: String,
    val itemType: String,
    val entityId: String,
    val order: Long,
)
