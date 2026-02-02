package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.tactical.TacticalMission
import org.junit.Assert.*
import org.junit.Test

class SyncMapperTest {

    @Test
    fun `migrateV1ToV2 should generate attachments for checklists and documents`() {
        // 1. Готуємо фейкові дані старого формату (V1)
        val testContextId = "project-123"
        val legacyContent = DatabaseContent(
            checklists = listOf(
                ChecklistEntity(id = "ch-1", name = "Test Checklist", contextId = testContextId)
            ),
            documents = listOf(
                NoteDocumentEntity(id = "doc-1", name = "Test Note", contextId = testContextId, content = "Some text")
            ),
            tacticalMissions = listOf(
                TacticalMission(id = "mission-1", title = "Task", projectId = testContextId)
            )
        )

        // 2. Запускаємо міграцію
        val snapshot = SyncMapper.migrateV1ToV2(legacyContent)

        // 3. ПЕРЕВІРКИ

        // Перевірка документів
        assertEquals(1, snapshot.documents.size)
        assertEquals("Some text", snapshot.documents[0].content)

        // ПЕРЕВІРКА ЗВ'ЯЗКІВ (те саме, що фіксить "невидимість")
        // Має бути 2 вкладення: одне для чек-ліста, одне для документа
        assertEquals(2, snapshot.attachments.size)
        assertEquals(2, snapshot.crossRefs.size)

        // Перевірка детермінованості ID
        val expectedAttachmentId = SyncMapper.generateDeterministicId("ch-1", "CHECKLIST")
        val actualAttachmentId = snapshot.attachments.find { it.entityId == "ch-1" }?.id
        assertEquals("ID вкладення має бути детермінованим", expectedAttachmentId, actualAttachmentId)

        // Перевірка CrossRef
        val crossRef = snapshot.crossRefs.find { it.attachmentId == actualAttachmentId }
        assertNotNull("CrossRef має існувати для вкладення", crossRef)
        assertEquals(testContextId, crossRef?.contextId)
    }

    @Test
    fun `migrateV1ToV2 should handle empty contextId safely`() {
        val legacyContent = DatabaseContent(
            checklists = listOf(
                ChecklistEntity(id = "ch-orphan", name = "No Project", contextId = "")
            )
        )

        val snapshot = SyncMapper.migrateV1ToV2(legacyContent)

        // Якщо contextId порожній, вкладення не повинні створюватися (щоб не засмічувати базу)
        assertTrue(snapshot.attachments.isEmpty())
        assertTrue(snapshot.crossRefs.isEmpty())
    }
}