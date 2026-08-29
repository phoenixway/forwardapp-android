package com.romankozak.forwardappmobile.sync

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SyncFileServiceSnapshotTest {
    private lateinit var syncFileService: SyncFileService

    // Оголошення моків
    private val mockContentProvider: IContentProvider = mockk()
    private val mockLocalDataSource: FullBackupLocalDataSource = mockk()
    private val mockMergeRepository: MergeRepository = mockk()

    private val gson = GsonBuilder().create()

    @Before
    fun setup() {
        syncFileService =
            SyncFileService(
                contentProvider = mockContentProvider,
                localDataSource = mockLocalDataSource,
                mergeRepository = mockMergeRepository,
            )
    }

    // === Допоміжні методи для створення JSON (Без скорочень) ===

    private fun createNewFormatJson(): String {
        val snapshot =
            SnapshotBundle(
                version = 2,
                contexts =
                    listOf(
                        ContextSnapshot(
                            id = "new_c1", name = "New Context", createdAt = 100L, updatedAt = 100L,
                            isExpanded = false, isDeleted = false, version = 1, contextStatus = "NO_PLAN",
                            contextLogLevel = null, isContextManagementEnabled = false,
                            parentId = null, description = null, contextStatusText = null,
                            tags = emptyList(), relatedLinks = emptyList(), order = 0, isAttachmentsExpanded = false,
                            defaultViewModeName = "DIRECTION", isCompleted = false, totalTimeSpentMinutes = 0L,
                            valueImportance = 0, valueImpact = 0, effort = 0, cost = 0, risk = 0,
                            weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0.0, displayScore = 0.0,
                            scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null,
                        ),
                    ),
            )
        val backup =
            FullAppBackup(
                backupSchemaVersion = 2,
                snapshotBundle = snapshot,
                settings = null,
            )
        return gson.toJson(backup)
    }

    private fun createNewFormatJsonWithoutExecutionStrictness(): String {
        val snapshot =
            SnapshotBundle(
                version = 2,
                contexts =
                    listOf(
                        ContextSnapshot(
                            id = "new_c1", name = "New Context", createdAt = 100L, updatedAt = 100L,
                            isExpanded = false, isDeleted = false, version = 1, contextStatus = "NO_PLAN",
                            contextLogLevel = null, isContextManagementEnabled = false,
                            parentId = null, description = null, contextStatusText = null,
                            tags = emptyList(), relatedLinks = emptyList(), order = 0, isAttachmentsExpanded = false,
                            defaultViewModeName = "DIRECTION", isCompleted = false, totalTimeSpentMinutes = 0L,
                            valueImportance = 0, valueImpact = 0, effort = 0, cost = 0, risk = 0,
                            weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0.0, displayScore = 0.0,
                            scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null,
                        ),
                    ),
                dayPlans = emptyList(),
                dayTasks =
                    listOf(
                        DayTaskSnapshot(
                            id = "task_1",
                            dayPlanId = "plan_1",
                            title = "Task",
                            description = null,
                            goalId = null,
                            projectId = null,
                            linkedProjectIds = emptyList(),
                            linkedAttachmentIds = emptyList(),
                            activityRecordId = null,
                            recurringTaskId = null,
                            taskType = "GOAL",
                            entityId = null,
                            order = 0,
                            priority = "MEDIUM",
                            status = "NOT_STARTED",
                            completed = false,
                            scheduledTime = null,
                            estimatedDurationMinutes = 15,
                            actualDurationMinutes = null,
                            dueTime = null,
                            executionStrictness = null,
                            valueImportance = 0f,
                            valueImpact = 0f,
                            effort = 0f,
                            cost = 0f,
                            risk = 0f,
                            location = null,
                            tags = emptyList(),
                            notes = null,
                            createdAt = 100L,
                            updatedAt = 100L,
                            isDeleted = false,
                            version = 1,
                            completedAt = null,
                            nextOccurrenceTime = null,
                            points = 0,
                        ),
                    ),
            )
        val backup =
            FullAppBackup(
                backupSchemaVersion = 2,
                snapshotBundle = snapshot,
                settings = null,
            )
        return gson.toJson(backup)
    }

    private fun createLegacyFormatJson(): String =
        """{
          "backupSchemaVersion": 1,
          "database": {
            "projects": [
              {
                "id": "legacy_c1",
                "name": "Legacy Context"
              }
            ]
          }
        }""".trimIndent()

    private fun createOldDatabaseContentJson(): String =
        """{
          "projects": [
            {
              "id": "raw_c1",
              "name": "Raw legacy database content"
            }
          ]
        }""".trimIndent()

    // === Тести ===

    @Test
    fun `importFullBackupFromFileV2 imports new snapshot format correctly`() =
        runBlocking {
            val uriString = "content://test/new_format"
            val jsonString = createNewFormatJson()

            every { mockContentProvider.readText(uriString) } returns Result.success(jsonString)
            coEvery { mockMergeRepository.applyServerChanges(any<SnapshotBundle>()) } returns Result.success(Unit)
            coEvery { mockLocalDataSource.restoreSettings(any()) } returns Unit

            val result = syncFileService.importFullBackupFromFileV2(uriString)

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 1) {
                mockMergeRepository.applyServerChanges(
                    match<SnapshotBundle> {
                        it.contexts.first().id == "new_c1" &&
                            it.contexts.first().defaultViewModeName == "DIRECTION"
                    },
                )
            }
        }

    @Test
    fun `importFullBackupFromFileV2 rejects legacy database-only FullAppBackup`() =
        runBlocking {
            val uriString = "content://test/legacy_format"
            val jsonString = createLegacyFormatJson()

            every { mockContentProvider.readText(uriString) } returns Result.success(jsonString)

            val result = syncFileService.importFullBackupFromFileV2(uriString)

            assertThat(result.isFailure).isTrue()
            coVerify(exactly = 0) { mockMergeRepository.applyServerChanges(any<SnapshotBundle>()) }
        }

    @Test
    fun `importFullBackupFromFileV2 defaults missing executionStrictness for old snapshot backups`() =
        runBlocking {
            val uriString = "content://test/old_snapshot_without_execution_strictness"
            val jsonString = createNewFormatJsonWithoutExecutionStrictness()

            every { mockContentProvider.readText(uriString) } returns Result.success(jsonString)
            coEvery { mockMergeRepository.applyServerChanges(any<SnapshotBundle>()) } returns Result.success(Unit)
            coEvery { mockLocalDataSource.restoreSettings(any()) } returns Unit

            val result = syncFileService.importFullBackupFromFileV2(uriString)

            assertThat(result.isSuccess).isTrue()
            coVerify(exactly = 1) {
                mockMergeRepository.applyServerChanges(
                    match<SnapshotBundle> {
                        it.dayTasks.single().executionStrictness == "NORMAL"
                    },
                )
            }
        }

    @Test
    fun `importFullBackupFromFileV2 rejects raw DatabaseContent`() =
        runBlocking {
            val uriString = "content://test/old_dbcontent_format"
            val jsonString = createOldDatabaseContentJson()

            every { mockContentProvider.readText(uriString) } returns Result.success(jsonString)

            val result = syncFileService.importFullBackupFromFileV2(uriString)

            assertThat(result.isFailure).isTrue()
            coVerify(exactly = 0) { mockMergeRepository.applyServerChanges(any<SnapshotBundle>()) }
        }

    @Test
    fun `importFullBackupFromFileV2 returns failure on IOException`() =
        runBlocking {
            val uriString = "content://test/non_existent_file"

            // Мокаємо повернення помилки з IContentProvider
            every { mockContentProvider.readText(uriString) } returns Result.failure(IOException("File not found"))

            val result = syncFileService.importFullBackupFromFileV2(uriString)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        }

    @Test
    fun `importFullBackupFromFileV2 returns failure on invalid JsonSyntaxException`() =
        runBlocking {
            val uriString = "content://test/invalid_json"

            every { mockContentProvider.readText(uriString) } returns Result.success("invalid json")

            val result = syncFileService.importFullBackupFromFileV2(uriString)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(JsonSyntaxException::class.java)
        }
}
