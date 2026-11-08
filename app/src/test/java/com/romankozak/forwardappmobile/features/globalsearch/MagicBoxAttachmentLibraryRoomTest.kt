package com.romankozak.forwardappmobile.features.globalsearch

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MagicBoxAttachmentLibraryRoomTest {
    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries().build()
    private suspend fun seed(db: AppDatabase, type: String, id: String, deleted: Boolean = false) {
        when (type) {
            "NOTE_DOCUMENT" -> db.noteDocumentDao().insertDocument(NoteDocumentEntity(id = id, name = "Note $id", content = "Body", isDeleted = deleted))
            "CHECKLIST" -> db.checklistDao().insertChecklist(ChecklistEntity(id = id, name = "List $id", isDeleted = deleted))
            "MUSIC_NOTE" -> db.musicNoteDao().insert(MusicNoteEntity(id = id, name = "Score $id", isDeleted = deleted))
            "SCRIPT" -> db.scriptDao().insert(ScriptEntity(id = id, name = "Script $id", content = "Body", isDeleted = deleted))
        }
        db.attachmentDao().insertAttachment(AttachmentEntity(id = "a-$id", entityId = id, attachmentType = type))
    }

    @Test fun `live unattached document list music and script are admitted without a Context shell`() = runBlocking<Unit> {
        val db = database()
        try {
            listOf("NOTE_DOCUMENT", "CHECKLIST", "MUSIC_NOTE", "SCRIPT").forEach { seed(db, it, it) }
            assertThat(db.attachmentDao().getLibraryItemsFlow().first().map { it.entityId })
                .containsExactly("NOTE_DOCUMENT", "CHECKLIST", "MUSIC_NOTE", "SCRIPT")
        } finally { db.close() }
    }

    @Test fun `tombstoned attachment target and missing document never enter live library search snapshot`() = runBlocking<Unit> {
        val db = database()
        try {
            seed(db, "NOTE_DOCUMENT", "live")
            listOf("NOTE_DOCUMENT", "CHECKLIST", "MUSIC_NOTE", "SCRIPT").forEach { seed(db, it, "dead-$it", deleted = true) }
            seed(db, "NOTE_DOCUMENT", "attachment-dead")
            db.attachmentDao().insertAttachment(AttachmentEntity(id = "a-attachment-dead", entityId = "attachment-dead",
                attachmentType = "NOTE_DOCUMENT", isDeleted = true))
            db.attachmentDao().insertAttachment(AttachmentEntity(id = "orphan", entityId = "absent", attachmentType = "NOTE_DOCUMENT"))
            assertThat(db.attachmentDao().getLibraryItemsFlow().first().map { it.entityId }).containsExactly("live")
            assertThat(db.noteDocumentDao().getDocumentById("dead-NOTE_DOCUMENT")!!.isDeleted).isTrue()
        } finally { db.close() }
    }

    @Test fun `rename content and deletion are reflected by observable library snapshots`() = runBlocking<Unit> {
        val db = database()
        try {
            seed(db, "NOTE_DOCUMENT", "note")
            val document = db.noteDocumentDao().getDocumentById("note")!!
            db.noteDocumentDao().updateDocument(document.copy(name = "Renamed", content = "New content"))
            val changed = db.attachmentDao().getLibraryItemsFlow().first().single()
            assertThat(changed.noteName).isEqualTo("Renamed")
            assertThat(changed.noteContent).isEqualTo("New content")
            db.noteDocumentDao().updateDocument(document.copy(isDeleted = true))
            assertThat(db.attachmentDao().getLibraryItemsFlow().first()).isEmpty()
        } finally { db.close() }
    }
}
