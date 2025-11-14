package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.repository

import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model.LegacyNote
import kotlinx.coroutines.flow.Flow

interface LegacyNotesRepository {
    fun observeNotes(projectId: String? = null): Flow<List<LegacyNote>>
    suspend fun getNoteById(id: String): LegacyNote?
    suspend fun insert(note: LegacyNote)
    suspend fun updateContent(id: String, title: String, content: String?, updatedAt: Long)
    suspend fun deleteById(id: String)
    suspend fun deleteAll(projectId: String? = null)
}
