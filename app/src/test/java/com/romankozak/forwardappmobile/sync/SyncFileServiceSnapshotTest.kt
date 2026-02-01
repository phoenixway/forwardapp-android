package com.romankozak.forwardappmobile.sync

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextSnapshot
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SyncFileServiceSnapshotTest {

    private lateinit var syncFileService: SyncFileService

    // Оголошення моків
    private val mockContentProvider: IContentProvider = mockk()
    private val mockLocalDataSource: FullBackupLocalDataSource = mockk()
    private val mockLegacyMigrationMapper: LegacyMigrationMapper = mockk()
    private val mockMergeRepository: MergeRepository = mockk()

    private val gson = GsonBuilder().create()

    @Before
    fun setup() {
        syncFileService = SyncFileService(
            contentProvider = mockContentProvider,
            localDataSource = mockLocalDataSource,
            legacyMigrationMapper = mockLegacyMigrationMapper,
            mergeRepository = mockMergeRepository
        )
    }

    // === Допоміжні методи для створення JSON (Без скорочень) ===

    private fun createNewFormatJson(): String {
        val snapshot = SnapshotBundle(
            version = 2,
            contexts = listOf(
                ContextSnapshot(
                    id = "new_c1", name = "New Context", createdAt = 100L, updatedAt = 100L,
                    isExpanded = false, isDeleted = false, version = 1, contextStatus = "NO_PLAN",
                    contextLogLevel = null, isContextManagementEnabled = false,
                    parentId = null, description = null, contextStatusText = null,
                    tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false,
                    defaultViewModeName = null, isCompleted = false, totalTimeSpentMinutes = null,
                    valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f,
                    weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0,
                    scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
                )
            )
        )
        val backup = FullAppBackup(
            backupSchemaVersion = 2,
            snapshotBundle = snapshot,
            database = null,
            settings = null
        )
        return gson.toJson(backup)
    }

    private fun createLegacyFormatJson(): String {
        val dbContent = DatabaseContent(
            projects = listOf(
                com.romankozak.forwardappmobile.core.data.models.Context(
                    id = "legacy_c1", name = "Legacy Context", createdAt = 50L, updatedAt = 50L,
                    parentId = null, description = null, isExpanded = false, isDeleted = false, version = 0,
                    tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false,
                    defaultViewModeName = null, isCompleted = false, isContextManagementEnabled = false,
                    contextStatus = "NO_PLAN", contextStatusText = null, contextLogLevel = null,
                    totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f,
                    cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f,
                    rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED",
                    showCheckboxes = false, roleCode = null
                )
            )
        )
        val backup = FullAppBackup(
            backupSchemaVersion = 1,
            database = dbContent,
            settings = null,
            snapshotBundle = null
        )
        return gson.toJson(backup)
    }

    private fun createOldDatabaseContentJson(): String {
        val dbContent = DatabaseContent(
            projects = listOf(
                com.romankozak.forwardappmobile.core.data.models.Context(
                    id = "raw_c1", name = "Raw DB Content", createdAt = 20L, updatedAt = 20L,
                    parentId = null, description = null, isExpanded = false, isDeleted = false, version = 0,
                    tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false,
                    defaultViewModeName = null, isCompleted = false, isContextManagementEnabled = false,
                    contextStatus = "NO_PLAN", contextStatusText = null, contextLogLevel = null,
                    totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f,
                    cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f,
                    rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED",
                    showCheckboxes = false, roleCode = null
                )
            )
        )
        return gson.toJson(dbContent)
    }

    // === Тести ===

    @Test
    fun `importFullBackupFromFileV2 imports new snapshot format correctly`() = runBlocking {
        val uriString = "content://test/new_format"
        val jsonString = createNewFormatJson()

        every { mockContentProvider.readText(uriString) } returns Result.success(jsonString)
        coEvery { mockMergeRepository.applyServerChanges(any<SnapshotBundle>()) } returns Result.success(Unit)
        coEvery { mockLocalDataSource.restoreSettings(any()) } returns Unit

        val result = syncFileService.importFullBackupFromFileV2(uriString)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) {
            mockMergeRepository.applyServerChanges(match<SnapshotBundle> { it.contexts.first().id == "new_c1" })
        }
    }

    @Test
    fun `importFullBackupFromFileV2 imports legacy FullAppBackup format correctly`() = runBlocking {
        val uriString = "content://test/legacy_format"
        val jsonString = createLegacyFormatJson()

        val migratedSnapshot = SnapshotBundle(
            version = 1,
            contexts = listOf(
                ContextSnapshot(
                    id = "legacy_c1", name = "Legacy Context", createdAt = 50L, updatedAt = 50L,
                    isExpanded = false, isDeleted = false, version = 0, contextStatus = "NO_PLAN",
                    contextLogLevel = null, isContextManagementEnabled = false,
                    parentId = null, description = null, contextStatusText = null,
                    tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false,
                    defaultViewModeName = null, isCompleted = false, totalTimeSpentMinutes = null,
                    valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f,
                    weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0,
                    scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
                )
            )
        )

        every { mockContentProvider.readText(uriString) } returns Result.success(jsonString)
        every { mockLegacyMigrationMapper.toSnapshotBundle(any<DatabaseContent>()) } returns migratedSnapshot
        coEvery { mockMergeRepository.applyServerChanges(any<SnapshotBundle>()) } returns Result.success(Unit)
        coEvery { mockLocalDataSource.restoreSettings(any()) } returns Unit

        val result = syncFileService.importFullBackupFromFileV2(uriString)

        assertThat(result.isSuccess).isTrue()
        verify(exactly = 1) { mockLegacyMigrationMapper.toSnapshotBundle(any<DatabaseContent>()) }
        coVerify(exactly = 1) { mockMergeRepository.applyServerChanges(migratedSnapshot) }
    }

    @Test
    fun `importFullBackupFromFileV2 imports raw DatabaseContent format correctly`() = runBlocking {
        val uriString = "content://test/old_dbcontent_format"
        val jsonString = createOldDatabaseContentJson()

        val migratedSnapshot = SnapshotBundle(
            version = 1,
            contexts = listOf(
                ContextSnapshot(
                    id = "raw_c1", name = "Raw DB Content", createdAt = 20L, updatedAt = 20L,
                    isExpanded = false, isDeleted = false, version = 0, contextStatus = "NO_PLAN",
                    contextLogLevel = null, isContextManagementEnabled = false,
                    parentId = null, description = null, contextStatusText = null,
                    tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false,
                    defaultViewModeName = null, isCompleted = false, totalTimeSpentMinutes = null,
                    valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f,
                    weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0,
                    scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
                )
            )
        )

        every { mockContentProvider.readText(uriString) } returns Result.success(jsonString)
        every { mockLegacyMigrationMapper.toSnapshotBundle(any<DatabaseContent>()) } returns migratedSnapshot
        coEvery { mockMergeRepository.applyServerChanges(any<SnapshotBundle>()) } returns Result.success(Unit)
        coEvery { mockLocalDataSource.restoreSettings(any()) } returns Unit

        val result = syncFileService.importFullBackupFromFileV2(uriString)

        assertThat(result.isSuccess).isTrue()
        verify(exactly = 1) { mockLegacyMigrationMapper.toSnapshotBundle(any<DatabaseContent>()) }
        coVerify(exactly = 1) { mockMergeRepository.applyServerChanges(migratedSnapshot) }
    }

    @Test
    fun `importFullBackupFromFileV2 returns failure on IOException`() = runBlocking {
        val uriString = "content://test/non_existent_file"

        // Мокаємо повернення помилки з IContentProvider
        every { mockContentProvider.readText(uriString) } returns Result.failure(IOException("File not found"))

        val result = syncFileService.importFullBackupFromFileV2(uriString)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
    }

    @Test
    fun `importFullBackupFromFileV2 returns failure on invalid JsonSyntaxException`() = runBlocking {
        val uriString = "content://test/invalid_json"

        every { mockContentProvider.readText(uriString) } returns Result.success("invalid json")

        val result = syncFileService.importFullBackupFromFileV2(uriString)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(JsonSyntaxException::class.java)
    }
}