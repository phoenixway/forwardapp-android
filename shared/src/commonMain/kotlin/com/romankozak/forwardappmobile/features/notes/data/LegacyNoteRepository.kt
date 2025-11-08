package com.romankozak.forwardappmobile.features.notes.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.Notes
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLink
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLinkDataSource
import com.romankozak.forwardappmobile.shared.features.notes.data.model.LegacyNote
import com.romankozak.forwardappmobile.shared.features.recentitems.data.RecentItemsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val LEGACY_NOTE_ITEM_TYPE = "NOTE"

private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

class LegacyNoteRepository(
    private val database: ForwardAppDatabase,
    private val backlogLinkDataSource: NoteBacklogLinkDataSource,
    private val recentItemsRepository: RecentItemsRepository,
    private val queryContext: CoroutineContext = EmptyCoroutineContext,
) {

    fun getNotesForProject(projectId: String): Flow<List<LegacyNote>> =
        database.legacyNoteQueries
            .getNotesForProject(projectId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllAsFlow(): Flow<List<LegacyNote>> =
        database.legacyNoteQueries
            .getAllLegacyNotes()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun getNoteById(noteId: String): LegacyNote? =
        withContext(queryContext) {
            database.legacyNoteQueries
                .getLegacyNoteById(noteId)
                .executeAsOneOrNull()
                ?.toModel()
        }

    suspend fun saveNote(note: LegacyNote) {
        val isNewNote =
            withContext(queryContext) {
                val existing = database.legacyNoteQueries.getLegacyNoteById(note.id).executeAsOneOrNull()
                if (existing == null) {
                    database.legacyNoteQueries.insertLegacyNote(
                        id = note.id,
                        projectId = note.projectId,
                        title = note.title,
                        content = note.content,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt,
                    )
                    backlogLinkDataSource.insertLink(
                        NoteBacklogLink(
                            id = uuid4().toString(),
                            projectId = note.projectId,
                            itemType = LEGACY_NOTE_ITEM_TYPE,
                            entityId = note.id,
                            order = -currentTimeMillis(),
                        ),
                    )
                    true
                } else {
                    database.legacyNoteQueries.updateLegacyNote(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        updatedAt = note.updatedAt,
                    )
                    false
                }
            }

        if (!isNewNote) {
            recentItemsRepository.updateRecentItemDisplayName(note.id, note.title)
        }
    }

    suspend fun deleteNote(noteId: String) {
        withContext(queryContext) { database.legacyNoteQueries.deleteLegacyNoteById(noteId) }
        backlogLinkDataSource.deleteLinkByEntityId(noteId)
    }

    suspend fun replaceAll(notes: List<LegacyNote>) {
        withContext(queryContext) {
            database.transaction {
                database.legacyNoteQueries.deleteAllLegacyNotes()
                notes.forEach { note ->
                    database.legacyNoteQueries.insertLegacyNote(
                        id = note.id,
                        projectId = note.projectId,
                        title = note.title,
                        content = note.content,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt,
                    )
                }
            }
        }
    }

    suspend fun deleteAll() {
        withContext(queryContext) { database.legacyNoteQueries.deleteAllLegacyNotes() }
    }

    suspend fun getAllSnapshot(): List<LegacyNote> =
        withContext(queryContext) {
            database.legacyNoteQueries
                .getAllLegacyNotes()
                .executeAsList()
                .map { it.toModel() }
        }

    private fun Notes.toModel(): LegacyNote =
        LegacyNote(
            id = id,
            projectId = projectId,
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}