package com.romankozak.forwardappmobile.sync

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonSyntaxException
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.FullAppBackup
import com.romankozak.forwardappmobile.core.data.models.sync.SettingsContent
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toSnapshot
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class SyncFileServiceSnapshotTest {

    private lateinit var syncFileService: SyncFileService
    private val mockContext: Context = mockk()
    private val mockLocalDataSource: FullBackupLocalDataSource = mockk()
    private val mockLegacyMigrationMapper: LegacyMigrationMapper = mockk()
    private val mockMergeRepository: MergeRepository = mockk()

    private val gson = com.google.gson.GsonBuilder().create()

    @Before
    fun setup() {
        syncFileService = SyncFileService(
            context = mockContext,
            localDataSource = mockLocalDataSource,
            legacyMigrationMapper = mockLegacyMigrationMapper,
            mergeRepository = mockMergeRepository
        )

        // Mocking contentResolver for readTextFromUri
        val mockContentResolver = mockk<android.content.ContentResolver>()
        coEvery { mockContext.contentResolver } returns mockContentResolver
        coEvery { mockContentResolver.openInputStream(any()) } answers {
            val uri = firstArg<Uri>()
            val jsonString = when (uri.toString()) {
                "content://test/new_format" -> createNewFormatJson()
                "content://test/legacy_format" -> createLegacyFormatJson()
                "content://test/old_dbcontent_format" -> createOldDatabaseContentJson()
                else -> throw IllegalArgumentException("Unknown URI: $uri")
            }
            ByteArrayInputStream(jsonString.toByteArray())
        }
    }

    private fun createNewFormatJson(): String {
        val snapshot = SnapshotBundle(
            version = 2,
            contexts = listOf(ContextSnapshot(
                id = "new_c1", name = "New Context", createdAt = 100L, updatedAt = 100L,
                isExpanded = false, isDeleted = false, version = 1, contextStatus = "NO_PLAN",
                contextLogLevel = null, isContextManagementEnabled = false,
                parentId = null, description = null, contextStatusText = null,
                tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false, defaultViewModeName = null, isCompleted = false, totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
            ))
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
            projects = listOf(com.romankozak.forwardappmobile.core.data.models.Context(
                id = "legacy_c1", name = "Legacy Context", createdAt = 50L, updatedAt = 50L,
                parentId = null, description = null, isExpanded = false, isDeleted = false, version = 0,
                tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false, defaultViewModeName = null, isCompleted = false, isContextManagementEnabled = false, contextStatus = "NO_PLAN", contextStatusText = null, contextLogLevel = null, totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
            ))
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
            projects = listOf(com.romankozak.forwardappmobile.core.data.models.Context(
                id = "raw_c1", name = "Raw DB Content", createdAt = 20L, updatedAt = 20L,
                parentId = null, description = null, isExpanded = false, isDeleted = false, version = 0,
                tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false, defaultViewModeName = null, isCompleted = false, isContextManagementEnabled = false, contextStatus = "NO_PLAN", contextStatusText = null, contextLogLevel = null, totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
            ))
        )
        return gson.toJson(dbContent) // This simulates a file that is just DatabaseContent
    }

    @Test
    fun `importFullBackupFromFileV2 imports new snapshot format correctly`() = runBlocking {
        // Given
        val uri = Uri.parse("content://test/new_format")
        val expectedSnapshot = SnapshotBundle(
            version = 2,
            contexts = listOf(ContextSnapshot(
                id = "new_c1", name = "New Context", createdAt = 100L, updatedAt = 100L,
                isExpanded = false, isDeleted = false, version = 1, contextStatus = "NO_PLAN",
                contextLogLevel = null, isContextManagementEnabled = false,
                parentId = null, description = null, contextStatusText = null,
                tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false, defaultViewModeName = null, isCompleted = false, totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
            ))
        )

        coEvery { mockMergeRepository.applyServerChanges(any()) } returns Result.success(Unit)

        // When
        val result = syncFileService.importFullBackupFromFileV2(uri)

        // Then
        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { mockMergeRepository.applyServerChanges(match { it.contexts.first().id == "new_c1" }) }
        coVerify(exactly = 0) { mockLegacyMigrationMapper.toSnapshotBundle(any()) } // Should not call mapper for new format
    }

    @Test
    fun `importFullBackupFromFileV2 imports legacy FullAppBackup format correctly`() = runBlocking {
        // Given
        val uri = Uri.parse("content://test/legacy_format")
        val migratedSnapshot = SnapshotBundle(
            version = 1,
            contexts = listOf(ContextSnapshot(
                id = "legacy_c1", name = "Legacy Context", createdAt = 50L, updatedAt = 50L,
                isExpanded = false, isDeleted = false, version = 0, contextStatus = "NO_PLAN",
                contextLogLevel = null, isContextManagementEnabled = false,
                parentId = null, description = null, contextStatusText = null,
                tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false, defaultViewModeName = null, isCompleted = false, totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
            ))
        )

        coEvery { mockLegacyMigrationMapper.toSnapshotBundle(any()) } returns migratedSnapshot
        coEvery { mockMergeRepository.applyServerChanges(any()) } returns Result.success(Unit)

        // When
        val result = syncFileService.importFullBackupFromFileV2(uri)

        // Then
        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { mockLegacyMigrationMapper.toSnapshotBundle(match { it.projects.first().id == "legacy_c1" }) }
        coVerify(exactly = 1) { mockMergeRepository.applyServerChanges(match { it.contexts.first().id == "legacy_c1" }) }
    }

    @Test
    fun `importFullBackupFromFileV2 imports raw DatabaseContent format correctly`() = runBlocking {
        // Given
        val uri = Uri.parse("content://test/old_dbcontent_format")
        val migratedSnapshot = SnapshotBundle(
            version = 1,
            contexts = listOf(ContextSnapshot(
                id = "raw_c1", name = "Raw DB Content", createdAt = 20L, updatedAt = 20L,
                isExpanded = false, isDeleted = false, version = 0, contextStatus = "NO_PLAN",
                contextLogLevel = null, isContextManagementEnabled = false,
                parentId = null, description = null, contextStatusText = null,
                tags = emptyList(), relatedLinks = emptyList(), order = 0L, isAttachmentsExpanded = false, defaultViewModeName = null, isCompleted = false, totalTimeSpentMinutes = null, valueImportance = 0f, valueImpact = 0f, effort = 0f, cost = 0f, risk = 0f, weightEffort = 1f, weightCost = 1f, weightRisk = 1f, rawScore = 0f, displayScore = 0, scoringStatus = "NOT_ASSESSED", showCheckboxes = false, roleCode = null
            ))
        )

        coEvery { mockLegacyMigrationMapper.toSnapshotBundle(any()) } returns migratedSnapshot
        coEvery { mockMergeRepository.applyServerChanges(any()) } returns Result.success(Unit)

        // When
        val result = syncFileService.importFullBackupFromFileV2(uri)

        // Then
        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { mockLegacyMigrationMapper.toSnapshotBundle(match { it.projects.first().id == "raw_c1" }) }
        coVerify(exactly = 1) { mockMergeRepository.applyServerChanges(match { it.contexts.first().id == "raw_c1" }) }
    }

    @Test
    fun `importFullBackupFromFileV2 returns failure on IOException`() = runBlocking {
        // Given
        val uri = Uri.parse("content://test/non_existent_file")
        coEvery { mockContext.contentResolver.openInputStream(any()) } returns null

        // When
        val result = syncFileService.importFullBackupFromFileV2(uri)

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
    }

    @Test
    fun `importFullBackupFromFileV2 returns failure on invalid JsonSyntaxException`() = runBlocking {
        // Given
        val uri = Uri.parse("content://test/new_format") // Use an existing URI
        coEvery { mockContext.contentResolver.openInputStream(any()) } returns ByteArrayInputStream("invalid json".toByteArray())

        // When
        val result = syncFileService.importFullBackupFromFileV2(uri)

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(JsonSyntaxException::class.java)
    }
}
