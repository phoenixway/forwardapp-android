package com.romankozak.forwardappmobile.ui.screens.selectiveimport

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
import com.romankozak.forwardappmobile.data.repository.SyncRepository
import com.romankozak.forwardappmobile.data.sync.BackupDiff
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.data.sync.DiffResult
import com.romankozak.forwardappmobile.data.sync.FullAppBackup
import com.romankozak.forwardappmobile.data.sync.RecentProjectEntry
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelectiveImportViewModelTest {

    @Test
    fun `selective import filters scripts and attachments by selected projects`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
        val project1 = Project(id = "p1", name = "P1", description = null, parentId = null, createdAt = 1L, updatedAt = 2L)
        val project2 = Project(id = "p2", name = "P2", description = null, parentId = null, createdAt = 1L, updatedAt = 2L)
        val attachment = AttachmentEntity(
            id = "att1",
            attachmentType = "NOTE_DOCUMENT",
            entityId = "doc1",
            ownerProjectId = project1.id,
            createdAt = 10L,
            updatedAt = 11L
        )
        val crossRef = ProjectAttachmentCrossRef(
            projectId = project1.id,
            attachmentId = attachment.id,
            attachmentOrder = 0
        )
        val scriptForP1 = ScriptEntity(id = "s1", projectId = project1.id, name = "Script 1", content = "println(1)", createdAt = 20L, updatedAt = 21L)
        val scriptForP2 = ScriptEntity(id = "s2", projectId = project2.id, name = "Script 2", content = "println(2)", createdAt = 20L, updatedAt = 21L)

        val databaseContent = DatabaseContent(
            projects = listOf(project1, project2),
            goals = emptyList(),
            listItems = emptyList(),
            legacyNotes = emptyList(),
            documents = emptyList(),
            documentItems = emptyList(),
            checklists = emptyList(),
            checklistItems = emptyList(),
            linkItemEntities = emptyList(),
            inboxRecords = emptyList(),
            projectExecutionLogs = emptyList(),
            scripts = listOf(scriptForP1, scriptForP2),
            activityRecords = emptyList(),
            attachments = listOf(attachment),
            projectAttachmentCrossRefs = listOf(crossRef),
            recentProjectEntries = listOf(RecentProjectEntry(projectId = project1.id, timestamp = 30L))
        )

        val backupDiff = BackupDiff(
            projects = DiffResult(added = databaseContent.projects),
            goals = DiffResult(),
            listItems = DiffResult(),
            legacyNotes = DiffResult(),
            activityRecords = DiffResult(),
            documents = DiffResult(),
            documentItems = DiffResult(),
            checklists = DiffResult(),
            checklistItems = DiffResult(),
            linkItems = DiffResult(),
            inboxRecords = DiffResult(),
            projectExecutionLogs = DiffResult(),
            scripts = DiffResult(added = databaseContent.scripts),
            attachments = DiffResult(added = databaseContent.attachments),
            projectAttachmentCrossRefs = DiffResult(added = databaseContent.projectAttachmentCrossRefs)
        )

        val syncRepository: SyncRepository = mockk(relaxed = true)
        coEvery { syncRepository.parseBackupFile(any<Uri>()) } returns Result.success(FullAppBackup(database = databaseContent))
        coEvery { syncRepository.createBackupDiff(any()) } returns backupDiff

        val importedContent = slot<DatabaseContent>()
        coEvery { syncRepository.importSelectedData(capture(importedContent)) } returns Result.success("ok")

        val savedStateHandle = SavedStateHandle(mapOf("fileUri" to "file://dummy"))
        val viewModel = SelectiveImportViewModel(
            syncRepository = syncRepository,
            savedStateHandle = savedStateHandle,
            application = mockk<Application>(relaxed = true)
        )

        advanceUntilIdle()

        // Deselect second project so scripts/crossRefs tied to it are excluded
        viewModel.toggleProjectSelection(project2.id, false)
        advanceUntilIdle()

        viewModel.onImportClicked()
        advanceUntilIdle()

        val result = importedContent.captured

        assertEquals(setOf(project1.id), result.projects.map { it.id }.toSet())
        assertTrue(result.scripts.any { it.id == scriptForP1.id })
        assertFalse(result.scripts.any { it.id == scriptForP2.id })
        assertEquals(listOf(crossRef), result.projectAttachmentCrossRefs)
        assertEquals(listOf(attachment), result.attachments)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
