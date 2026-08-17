package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.ResolvedWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import com.romankozak.forwardappmobile.shared.domain.contexts.WorkspaceSnapshotResolver
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class DesktopWorkspaceFileStore(
    private val workspaceFile: Path = defaultWorkspaceFile(),
    private val fixtureLoader: DesktopWorkspaceFixtureLoader = ClasspathDesktopWorkspaceFixtureLoader(),
    private val clock: Clock = Clock.systemUTC(),
    private val backupLimit: Int = DEFAULT_BACKUP_LIMIT,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true },
) {
    private val snapshotResolver = WorkspaceSnapshotResolver(json = json)
    private val snapshotMerger = DesktopWorkspaceSnapshotMerger()
    private val dayMaterialEnsurer = DesktopDayMaterialEnsurer()

    fun workspacePath(): Path = workspaceFile

    fun readSnapshot(): String {
        ensureSeededSnapshot()
        return workspaceFile.readText()
    }

    fun writeSnapshot(snapshotText: String) {
        ensureSeededSnapshot()
        createBackup()
        workspaceFile.writeText(snapshotText)
    }

    fun listBackups(): List<Path> =
        backupDirectory()
            .takeIf { directory -> directory.exists() }
            ?.listDirectoryEntries("desktop-workspace-*.json")
            ?.sortedByDescending { path -> path.name }
            .orEmpty()

    fun restoreLatestBackup(): Boolean {
        ensureSeededSnapshot()
        val latestBackup = listBackups().firstOrNull() ?: return false
        return restoreBackup(latestBackup)
    }

    fun restoreBackup(backupFile: Path): Boolean {
        ensureSeededSnapshot()
        if (!backupFile.exists()) {
            return false
        }
        createBackup()
        backupFile.copyTo(target = workspaceFile, overwrite = true)
        return true
    }

    fun exportSnapshot(targetFile: Path) {
        ensureSeededSnapshot()
        targetFile.parent?.createDirectories()
        workspaceFile.copyTo(target = targetFile, overwrite = true)
    }

    fun importSnapshot(sourceFile: Path): ImportResult {
        ensureSeededSnapshot()
        if (!sourceFile.exists()) {
            return ImportResult.FileNotFound(sourceFile)
        }
        val snapshotText = sourceFile.readText()
        val resolvedSnapshot = resolveSnapshot(snapshotText) ?: return ImportResult.InvalidSnapshot(sourceFile)
        return try {
            createBackup()
            workspaceFile.writeText(
                json.encodeToString(
                    DesktopWorkspaceSnapshot.serializer(),
                    resolvedSnapshot.snapshot,
                ),
            )
            ImportResult.Success(sourceFile = sourceFile, format = resolvedSnapshot.format)
        } catch (_: Exception) {
            ImportResult.InvalidSnapshot(sourceFile)
        }
    }

    fun mergeSnapshotText(snapshotText: String): MergeImportResult {
        ensureSeededSnapshot()
        val resolvedSnapshot = resolveSnapshot(snapshotText) ?: return MergeImportResult.InvalidSnapshot
        return try {
            val localSnapshot = json.decodeFromString(DesktopWorkspaceSnapshot.serializer(), readSnapshot())
            val mergedSnapshot = snapshotMerger.merge(local = localSnapshot, incoming = resolvedSnapshot.snapshot)
            createBackup()
            workspaceFile.writeText(
                json.encodeToString(
                    DesktopWorkspaceSnapshot.serializer(),
                    mergedSnapshot,
                ),
            )
            MergeImportResult.Success(
                format = resolvedSnapshot.format,
                incomingDayPlans = resolvedSnapshot.snapshot.dayPlans.size,
                incomingDayFocusItems = resolvedSnapshot.snapshot.dayFocusItems.size,
                incomingDayTasks = resolvedSnapshot.snapshot.dayTasks.size,
                mergedDayPlans = mergedSnapshot.dayPlans.size,
                mergedDayFocusItems = mergedSnapshot.dayFocusItems.size,
                mergedDayTasks = mergedSnapshot.dayTasks.size,
            )
        } catch (_: Exception) {
            MergeImportResult.InvalidSnapshot
        }
    }

    fun ensureTodayDayMaterial(): Boolean {
        ensureSeededSnapshot()
        return try {
            val snapshot = json.decodeFromString(DesktopWorkspaceSnapshot.serializer(), readSnapshot())
            val updatedSnapshot = dayMaterialEnsurer.ensureToday(snapshot)
            if (updatedSnapshot == snapshot) {
                false
            } else {
                createBackup()
                workspaceFile.writeText(
                    json.encodeToString(
                        DesktopWorkspaceSnapshot.serializer(),
                        updatedSnapshot,
                    ),
                )
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun inspectSnapshot(sourceFile: Path): SnapshotInspectionResult {
        if (!sourceFile.exists()) {
            return SnapshotInspectionResult.FileNotFound(sourceFile)
        }
        val snapshotText = sourceFile.readText()
        val resolvedSnapshot = resolveSnapshot(snapshotText) ?: return SnapshotInspectionResult.Invalid(sourceFile)
        return try {
            val snapshot = resolvedSnapshot.snapshot
            SnapshotInspectionResult.Valid(
                sourceFile = sourceFile,
                contextsCount = snapshot.contexts.size,
                backlogItemsCount = snapshot.backlogItems.size,
                format = resolvedSnapshot.format,
            )
        } catch (_: Exception) {
            SnapshotInspectionResult.Invalid(sourceFile)
        }
    }

    fun defaultExportPath(): Path =
        workspaceFile.parent
            .resolve("exports")
            .resolve("desktop-workspace-export-${timestampSuffix()}.json")

    private fun ensureSeededSnapshot() {
        if (workspaceFile.exists()) {
            return
        }
        workspaceFile.parent?.createDirectories()
        workspaceFile.writeText(fixtureLoader.loadFixture())
    }

    private fun createBackup() {
        if (!workspaceFile.exists()) {
            return
        }
        val backupDirectory = backupDirectory()
        backupDirectory.createDirectories()
        val backupFile = backupDirectory.resolve("desktop-workspace-${timestampSuffix()}.json")
        workspaceFile.copyTo(target = backupFile, overwrite = true)
        pruneOldBackups(backupDirectory)
    }

    private fun pruneOldBackups(backupDirectory: Path) {
        if (backupLimit <= 0) {
            return
        }
        backupDirectory
            .listDirectoryEntries("desktop-workspace-*.json")
            .sortedByDescending { path -> path.name }
            .drop(backupLimit)
            .forEach { path -> path.toFile().delete() }
    }

    private fun backupDirectory(): Path = workspaceFile.parent.resolve("backups")

    private fun resolveSnapshot(snapshotText: String): ResolvedWorkspaceSnapshot? =
        snapshotResolver.resolve(snapshotText)

    private fun timestampSuffix(): String =
        BACKUP_TIMESTAMP_FORMATTER.format(Instant.now(clock))

    companion object {
        private val BACKUP_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                .withZone(ZoneOffset.UTC)

        private const val DEFAULT_BACKUP_LIMIT = 20

        fun defaultWorkspaceFile(): Path {
            val homeDirectory = System.getProperty("user.home").orEmpty()
            return Paths.get(homeDirectory, ".forwardapp-desktop", "workspace", "desktop-workspace.json")
        }
    }
}

sealed interface ImportResult {
    data class Success(
        val sourceFile: Path,
        val format: WorkspaceSnapshotFormat,
    ) : ImportResult

    data class FileNotFound(
        val sourceFile: Path,
    ) : ImportResult

    data class InvalidSnapshot(
        val sourceFile: Path,
    ) : ImportResult
}

sealed interface MergeImportResult {
    data class Success(
        val format: WorkspaceSnapshotFormat,
        val incomingDayPlans: Int,
        val incomingDayFocusItems: Int,
        val incomingDayTasks: Int,
        val mergedDayPlans: Int,
        val mergedDayFocusItems: Int,
        val mergedDayTasks: Int,
    ) : MergeImportResult

    data object InvalidSnapshot : MergeImportResult
}

sealed interface SnapshotInspectionResult {
    data class Valid(
        val sourceFile: Path,
        val contextsCount: Int,
        val backlogItemsCount: Int,
        val format: WorkspaceSnapshotFormat,
    ) : SnapshotInspectionResult

    data class FileNotFound(
        val sourceFile: Path,
    ) : SnapshotInspectionResult

    data class Invalid(
        val sourceFile: Path,
    ) : SnapshotInspectionResult
}

interface DesktopWorkspaceFixtureLoader {
    fun loadFixture(): String
}

class ClasspathDesktopWorkspaceFixtureLoader(
    private val resourcePath: String = "fixtures/desktop-workspace.json",
) : DesktopWorkspaceFixtureLoader {
    override fun loadFixture(): String =
        checkNotNull(javaClass.classLoader.getResource(resourcePath)) {
            "desktop workspace fixture is missing at $resourcePath"
        }.readText()
}
