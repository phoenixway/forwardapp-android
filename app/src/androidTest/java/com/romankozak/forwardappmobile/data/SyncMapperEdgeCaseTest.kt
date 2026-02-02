package com.romankozak.forwardappmobile.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.sync.SyncMapper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncMapperEdgeCaseTest {
    @Test
    fun migrateV1ToV2_withOrphanedChecklist_shouldNotCreateAttachment() {
        // КЕЙС: Чек-ліст без прив'язки до проекту (contextId порожній)
        // Система не повинна створювати "бите" вкладення
        val legacy =
            DatabaseContent(
                checklists =
                    listOf(
                        ChecklistEntity(id = "orphan-1", name = "Orphan", contextId = ""),
                    ),
            )

        val bundle = SyncMapper.migrateV1ToV2(legacy)

        assertTrue(
            "Вкладення не повинні створюватися для порожніх contextId",
            bundle.attachments.isEmpty(),
        )
        assertTrue(
            "CrossRefs не повинні створюватися для порожніх contextId",
            bundle.crossRefs.isEmpty(),
        )
    }

    @Test
    fun migrateV1ToV2_withMalformedTimestamps_shouldSanitize() {
        // КЕЙС: updatedAt дорівнює 0 або null (що часто буває в старих бекапах)
        val now = System.currentTimeMillis()
        val legacy =
            DatabaseContent(
                projects =
                    listOf(
                        Context(id = "p1", name = "Project", createdAt = now, updatedAt = null),
                    ),
            )

        val bundle = SyncMapper.migrateV1ToV2(legacy)
        val migratedProject = bundle.contexts.first()

        assertNotNull("updatedAt не повинно бути null після міграції", migratedProject.updatedAt)
        assertTrue(
            "Якщо updatedAt був null, він має стати рівним createdAt",
            migratedProject.updatedAt >= now,
        )
    }

    @Test
    fun migrateV1ToV2_withComplexActivityRecords_shouldPreserveGameMechanics() {
        // КЕЙС: Перевірка нових полів XP/Anti-XP, які ми нещодавно лагодили
        val legacy =
            DatabaseContent(
                activityRecords =
                    listOf(
                        ActivityRecord(
                            id = "act-1",
                            text = "Training",
                            startTime = 1000L,
                            xpGained = 50,
                            antyXp = 10,
                            isDeleted = false,
                        ),
                    ),
            )

        val bundle = SyncMapper.migrateV1ToV2(legacy)
        val record = bundle.activityRecords.first()

        assertEquals("Поле text (ActivityRecord) має бути збережене", "Training", record.text)
        assertEquals("XP має бути успішно мігровано", 50, record.xpGained)
        assertEquals("Anti-XP має бути успішно мігровано", 10, record.antyXp)
    }

    @Test
    fun deterministicId_shouldBeConsistentAcrossRuns() {
        // КЕЙС: Гарантуємо, що той самий об'єкт завжди отримує той самий ID вкладення
        val entityId = "note-999"
        val type = "NOTE_DOCUMENT"

        val id1 = SyncMapper.generateDeterministicId(entityId, type)
        val id2 = SyncMapper.generateDeterministicId(entityId, type)

        assertEquals("ID має бути ідентичним при повторній генерації (IDempotency)", id1, id2)
    }

    @Test
    fun migrateV1ToV2_massiveDataLoad_performanceCheck() {
        // КЕЙС: Стрес-тест на 5000 записів (важливо для GitHub Actions і Termux)
        val massiveList =
            List(5000) { i ->
                ActivityRecord(id = "id-$i", text = "Log entry $i", startTime = System.currentTimeMillis())
            }
        val legacy = DatabaseContent(activityRecords = massiveList)

        val startTime = System.currentTimeMillis()
        val bundle = SyncMapper.migrateV1ToV2(legacy)
        val duration = System.currentTimeMillis() - startTime

        assertEquals(5000, bundle.activityRecords.size)
        // Якщо обробка 5к записів займає більше 1 секунди - це привід для оптимізації
        assertTrue("Міграція занадто повільна: ${duration}ms", duration < 1000)
    }

    @Test
    fun migrateV1ToV2_withMissingEntityInBundle_shouldHandleGracefully() {
        // КЕЙС: Посилання на проект є, але самого проекту в списку немає
        val legacy =
            DatabaseContent(
                projects = emptyList(), // Список порожній
                checklists =
                    listOf(
                        ChecklistEntity(id = "ch-1", name = "Test", contextId = "non-existent-p"),
                    ),
            )

        val bundle = SyncMapper.migrateV1ToV2(legacy)

        // Система має все одно створити вкладення, щоб дані не загубилися,
        // навіть якщо проект "загубився" (Dangling Reference)
        assertEquals(1, bundle.attachments.size)
        assertEquals("non-existent-p", bundle.attachments.first().ownerContextId)
    }
}
