package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogPriority
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import com.romankozak.forwardappmobile.shared.domain.contexts.WorkspaceSnapshotFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FileBasedDesktopWorkspaceRepositoryTest {
    @Test
    fun createUpdateAndDeleteOperationsPersistToWorkspaceFile() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-repo-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
            )
        val repository = FileBasedDesktopWorkspaceRepository(fileStore = fileStore)

        val createdContext =
            repository.createContext(
                parentId = "core",
                name = "Runtime Test Context",
                description = "Created from repository test",
                status = SharedContextStatus.InProgress,
                defaultView = SharedContextView.Dashboard,
            )

        assertNotNull(createdContext)
        assertTrue(repository.getContexts().any { context -> context.name == "Runtime Test Context" })

        val createdBacklogItem =
            repository.createBacklogItem(
                contextId = createdContext!!.id,
                title = "Repository persistence test",
                details = "Ensure create/update/delete round-trip works",
                priority = SharedBacklogPriority.High,
            )

        assertNotNull(createdBacklogItem)

        repository.updateBacklogItemContent(
            itemId = createdBacklogItem!!.id,
            title = "Repository persistence updated",
            details = "Updated details",
            priority = SharedBacklogPriority.Critical,
        )
        repository.updateBacklogItemDone(createdBacklogItem.id, isDone = true)

        val updatedBacklog =
            repository.getBacklogItems(createdContext.id).first { item -> item.id == createdBacklogItem.id }
        assertEquals("Repository persistence updated", updatedBacklog.title)
        assertEquals("Updated details", updatedBacklog.details)
        assertEquals(SharedBacklogPriority.Critical, updatedBacklog.priority)
        assertTrue(updatedBacklog.isDone)

        val updatedContext =
            repository.updateContext(
                contextId = createdContext.id,
                name = "Runtime Test Context Updated",
                description = "Edited context",
                status = SharedContextStatus.Completed,
                defaultView = SharedContextView.Direction,
            )

        assertNotNull(updatedContext)
        assertTrue(repository.getContexts().any { context -> context.name == "Runtime Test Context Updated" })

        assertTrue(repository.deleteBacklogItem(createdBacklogItem.id))
        assertTrue(repository.getBacklogItems(createdContext.id).isEmpty())

        assertTrue(repository.deleteContext(createdContext.id))
        assertFalse(repository.getContexts().any { context -> context.id == createdContext.id })
    }

    @Test
    fun deleteContextRemovesWholeSubtreeAndBacklogCascade() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-tree-delete-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_TREE_JSON),
            )
        val repository = FileBasedDesktopWorkspaceRepository(fileStore = fileStore)

        assertTrue(repository.deleteContext("project"))

        val remainingContexts = repository.getContexts()
        assertEquals(listOf("root"), remainingContexts.map { context -> context.id })
        assertTrue(repository.getBacklogItems("project").isEmpty())
        assertTrue(repository.getBacklogItems("child").isEmpty())

        val persistedSnapshot = fileStore.readSnapshot()
        assertFalse(persistedSnapshot.contains("\"project\""))
        assertFalse(persistedSnapshot.contains("\"child\""))
        assertFalse(persistedSnapshot.contains("\"project-item\""))
        assertFalse(persistedSnapshot.contains("\"child-item\""))
    }

    @Test
    fun fileStoreCreatesRotatingBackupsOnWrite() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-backup-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
                clock = SequenceClock("2026-01-01T00:00:00Z", "2026-01-01T00:00:01Z", "2026-01-01T00:00:02Z"),
                backupLimit = 2,
            )

        fileStore.writeSnapshot("{\"version\":1}")
        fileStore.writeSnapshot("{\"version\":2}")
        fileStore.writeSnapshot("{\"version\":3}")

        val backups = fileStore.listBackups()
        assertEquals(2, backups.size)
        assertTrue(backups.all { backup -> backup.fileName.toString().startsWith("desktop-workspace-") })
        assertEquals("{\"version\":3}", fileStore.readSnapshot())
        assertEquals("{\"version\":2}", backups.first().readText())
        assertEquals("{\"version\":1}", backups.last().readText())
    }

    @Test
    fun restoreLatestBackupReplacesCurrentSnapshotAndKeepsRecoveryHistory() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-restore-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
                clock =
                    SequenceClock(
                        "2026-01-01T00:00:00Z",
                        "2026-01-01T00:00:01Z",
                        "2026-01-01T00:00:02Z",
                        "2026-01-01T00:00:03Z",
                    ),
                backupLimit = 4,
            )

        fileStore.writeSnapshot("{\"version\":1}")
        fileStore.writeSnapshot("{\"version\":2}")
        fileStore.writeSnapshot("{\"version\":3}")

        assertTrue(fileStore.restoreLatestBackup())
        assertEquals("{\"version\":2}", fileStore.readSnapshot())

        val backups = fileStore.listBackups()
        assertTrue(backups.size >= 3)
        assertEquals("{\"version\":3}", backups.first().readText())
    }

    @Test
    fun restoreBackupUsesExplicitBackupFile() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-explicit-restore-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
                clock =
                    SequenceClock(
                        "2026-01-01T00:00:00Z",
                        "2026-01-01T00:00:01Z",
                        "2026-01-01T00:00:02Z",
                    ),
                backupLimit = 4,
            )

        fileStore.writeSnapshot("{\"version\":10}")
        fileStore.writeSnapshot("{\"version\":20}")

        val oldestBackup = fileStore.listBackups().last()
        assertTrue(fileStore.restoreBackup(oldestBackup))
        val restoredSnapshot = fileStore.readSnapshot()
        assertTrue(restoredSnapshot.contains("\"id\": \"core\""))
        assertFalse(restoredSnapshot.contains("\"version\":20"))
    }

    @Test
    fun exportSnapshotCopiesCurrentWorkspaceToTargetFile() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-export-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val exportFile = workspaceDir.resolve("exports").resolve("snapshot-export.json")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
            )

        fileStore.writeSnapshot("{\"version\":42}")
        fileStore.exportSnapshot(exportFile)

        assertTrue(Files.exists(exportFile))
        assertEquals("{\"version\":42}", exportFile.readText())
    }

    @Test
    fun importSnapshotReplacesWorkspaceAfterValidation() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-import-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val importFile = workspaceDir.resolve("imports").resolve("incoming.json")
        importFile.parent.createDirectories()
        importFile.writeText(WorkspaceSnapshotFixtures.DESKTOP_TREE_JSON)
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
                clock = SequenceClock("2026-01-01T00:00:00Z"),
                backupLimit = 4,
            )

        fileStore.writeSnapshot("{\"version\":99}")
        val result = fileStore.importSnapshot(importFile)

        assertTrue(result is ImportResult.Success)
        result as ImportResult.Success
        assertEquals(WorkspaceSnapshotFormat.Desktop, result.format)
        val snapshot = fileStore.readSnapshot()
        assertTrue(snapshot.contains("\"id\": \"project\""))
        assertFalse(snapshot.contains("\"version\":99"))
        assertTrue(fileStore.listBackups().isNotEmpty())
    }

    @Test
    fun importSnapshotAcceptsAndroidSnapshotBundleBackup() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-import-android-v2-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val importFile = workspaceDir.resolve("imports").resolve("android-v2.json")
        importFile.parent.createDirectories()
        importFile.writeText(WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON)
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
                clock = SequenceClock("2026-01-01T00:00:00Z"),
                backupLimit = 4,
            )

        val result = fileStore.importSnapshot(importFile)

        assertTrue(result is ImportResult.Success)
        result as ImportResult.Success
        assertEquals(WorkspaceSnapshotFormat.AndroidSnapshotBundleV2, result.format)
        val snapshot = fileStore.readSnapshot()
        assertTrue(snapshot.contains("\"title\": \"Ship desktop importer\""))
        assertTrue(snapshot.contains("\"title\": \"Architecture Notes\""))
        assertTrue(snapshot.contains("\"isDone\": true"))
    }

    @Test
    fun inspectSnapshotDetectsAndroidLegacyBackupMetadata() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-inspect-android-legacy-test")
        val importFile = workspaceDir.resolve("android-legacy.json")
        importFile.writeText(WorkspaceSnapshotFixtures.ANDROID_LEGACY_DATABASE_JSON)
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceDir.resolve("workspace.json"),
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
            )

        val result = fileStore.inspectSnapshot(importFile)

        assertTrue(result is SnapshotInspectionResult.Valid)
        result as SnapshotInspectionResult.Valid
        assertEquals(WorkspaceSnapshotFormat.AndroidLegacyDatabase, result.format)
        assertEquals(2, result.contextsCount)
        assertEquals(2, result.backlogItemsCount)
    }

    @Test
    fun importSnapshotRejectsInvalidJsonWithoutOverwritingWorkspace() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-import-invalid-test")
        val workspaceFile = workspaceDir.resolve("workspace.json")
        val importFile = workspaceDir.resolve("imports").resolve("broken.json")
        importFile.parent.createDirectories()
        importFile.writeText("{\"broken\":true}")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceFile,
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
                backupLimit = 4,
            )

        fileStore.writeSnapshot("{\"version\":88}")
        val result = fileStore.importSnapshot(importFile)

        assertTrue(result is ImportResult.InvalidSnapshot)
        assertEquals("{\"version\":88}", fileStore.readSnapshot())
    }

    @Test
    fun inspectSnapshotReturnsMetadataForValidWorkspaceFile() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-inspect-valid-test")
        val importFile = workspaceDir.resolve("incoming.json")
        importFile.writeText(WorkspaceSnapshotFixtures.DESKTOP_TREE_JSON)
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceDir.resolve("workspace.json"),
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
            )

        val result = fileStore.inspectSnapshot(importFile)

        assertTrue(result is SnapshotInspectionResult.Valid)
        result as SnapshotInspectionResult.Valid
        assertEquals(WorkspaceSnapshotFormat.Desktop, result.format)
        assertEquals(3, result.contextsCount)
        assertEquals(2, result.backlogItemsCount)
    }

    @Test
    fun inspectSnapshotDetectsMissingAndInvalidFiles() = runTest {
        val workspaceDir = Files.createTempDirectory("desktop-workspace-inspect-invalid-test")
        val missingFile = workspaceDir.resolve("missing.json")
        val invalidFile = workspaceDir.resolve("invalid.json")
        invalidFile.writeText("{\"broken\":true}")
        val fileStore =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceDir.resolve("workspace.json"),
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
            )

        assertTrue(fileStore.inspectSnapshot(missingFile) is SnapshotInspectionResult.FileNotFound)
        assertTrue(fileStore.inspectSnapshot(invalidFile) is SnapshotInspectionResult.Invalid)
    }

    private class StaticFixtureLoader(
        private val fixture: String,
    ) : DesktopWorkspaceFixtureLoader {
        override fun loadFixture(): String = fixture
    }

    private class SequenceClock(
        vararg timestamps: String,
    ) : Clock() {
        private val instants = timestamps.map(Instant::parse)
        private var index = 0

        override fun getZone(): ZoneOffset = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?): Clock = this

        override fun instant(): Instant {
            val currentIndex = index.coerceAtMost(instants.lastIndex)
            index += 1
            return instants[currentIndex]
        }
    }

    private companion object {
    }
}
