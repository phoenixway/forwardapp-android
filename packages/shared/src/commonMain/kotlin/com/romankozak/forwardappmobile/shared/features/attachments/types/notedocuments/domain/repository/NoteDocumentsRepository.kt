package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository

import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocument
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model.NoteDocumentItem
import kotlinx.coroutines.flow.Flow

interface NoteDocumentsRepository {
    fun observeDocuments(projectId: String? = null): Flow<List<NoteDocument>>
    fun observeDocumentItems(documentId: String): Flow<List<NoteDocumentItem>>

    suspend fun getDocumentById(id: String): NoteDocument?
    suspend fun getDocumentItemById(id: String): NoteDocumentItem?

    suspend fun upsertDocument(document: NoteDocument)
    suspend fun updateDocumentContent(
        id: String,
        name: String,
        content: String?,
        updatedAt: Long,
        lastCursorPosition: Long,
    )

    suspend fun upsertDocumentItem(item: NoteDocumentItem)

    suspend fun deleteDocumentById(id: String)
    suspend fun deleteDocuments(projectId: String? = null)

    suspend fun deleteDocumentItemById(id: String)
    suspend fun deleteDocumentItemsByDocument(documentId: String)
    suspend fun deleteDocumentItemsByIds(ids: List<String>)
}
