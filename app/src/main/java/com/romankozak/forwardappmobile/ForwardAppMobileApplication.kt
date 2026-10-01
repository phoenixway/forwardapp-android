package com.romankozak.forwardappmobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.storage.getDocumentsLogsDir
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyAuthorityActivator
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.capability.ExecutionLogWorkspaceOwnershipBridge
import com.romankozak.forwardappmobile.data.logic.TagAssociationHandler
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.logging.CoroutineFileTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ForwardAppMobileApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var tagAssociationHandler: TagAssociationHandler

    @Inject lateinit var canonicalDayThemeBootstrapper: CanonicalDayThemeBootstrapper

    @Inject lateinit var canonicalOrientationBootstrapper: CanonicalOrientationBootstrapper

    @Inject lateinit var canonicalWorkspaceBootstrapper: CanonicalWorkspaceBootstrapper

    @Inject lateinit var canonicalHierarchyAuthorityActivator: CanonicalHierarchyAuthorityActivator

    @Inject lateinit var databaseInitializer: DatabaseInitializer

    @Inject lateinit var executionLogWorkspaceOwnershipBridge: ExecutionLogWorkspaceOwnershipBridge

    @Inject lateinit var contextRepository: ContextRepository

    private val appScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        StartupTrace.mark("Application.onCreate.begin")

        // 1️⃣ ЛОГЕР ПЕРШИМ
        val logsDir =
            StartupTrace.measureSync("Application.logsDir") {
                applicationContext.getDocumentsLogsDir()
            }
        val fileTree =
            StartupTrace.measureSync("Application.loggerConstruct") {
                CoroutineFileTree(logsDir)
            }

        StartupTrace.measureSync("Application.loggerPlant") {
            Timber.plant(
                Timber.DebugTree(),
                fileTree,
            )
        }

        Timber.i("Logger initialized (Android 15)")

        /*
         * P2 authority establishment is deliberately synchronous and fail-closed.
         *
         * The finite CURRENT V1 snapshot is valid only after its canonical target
         * prerequisites exist: reserved System Workspace ownership, canonical
         * Orientation CUT_OVER mappings, and the canonical Workspace projection.
         * Therefore V2 startup establishes those prerequisites before the one-shot
         * hierarchy transaction and does not release UI/sync startup in between.
         *
         * Schema 180 has no pre-cutover runtime authority branch.
         */
        runBlocking(Dispatchers.IO) {
            StartupTrace.measure("Application.systemWorkspaceOwnership") {
                databaseInitializer.ensureCanonicalSystemWorkspaceOwnership()
            }

            val dayThemeReport =
                StartupTrace.measure("Application.dayThemeBootstrap") {
                    canonicalDayThemeBootstrapper.ensureBootstrapped()
                }
            if (dayThemeReport.performed) {
                Timber.i(
                    "Canonical DayTheme bootstrap completed: definitions=%d dayThemes=%d assignments=%d diagnostics=%d",
                    dayThemeReport.insertedThemeDefinitions,
                    dayThemeReport.insertedDayThemes,
                    dayThemeReport.insertedAssignmentDocuments,
                    dayThemeReport.diagnostics.size,
                )
            }

            val orientationReport =
                StartupTrace.measure("Application.orientationBootstrap") {
                    canonicalOrientationBootstrapper.ensureBootstrapped()
                }
            if (orientationReport.performed) {
                Timber.i(
                    "Canonical Orientation bootstrap: materialized=%d compared=%d issues=%d",
                    orientationReport.materialized,
                    orientationReport.compared,
                    orientationReport.issues.size,
                )
            }
            orientationReport.issues.forEach { issue ->
                Timber.e(
                    "Canonical Orientation bootstrap issue: source=%s:%s code=%s detail=%s",
                    issue.sourceType,
                    issue.sourceId,
                    issue.code,
                    issue.detail,
                )
            }
            check(orientationReport.issues.isEmpty()) {
                "P2 hierarchy authority activation requires COMPLETE canonical Orientation bootstrap; " +
                    "issues=${orientationReport.issues.size}"
            }

            val workspaceReport =
                StartupTrace.measure("Application.workspaceBootstrap") {
                    canonicalWorkspaceBootstrapper.ensureBootstrapped()
                }
            if (workspaceReport.performed || workspaceReport.issues.isNotEmpty()) {
                Timber.i(
                    "Canonical Workspace bootstrap: workspaces=%d capabilities=%d issues=%d",
                    workspaceReport.projectedWorkspaces,
                    workspaceReport.projectedCapabilities,
                    workspaceReport.issues.size,
                )
            }
            check(workspaceReport.issues.isEmpty()) {
                "P2 hierarchy authority activation requires COMPLETE canonical Workspace bootstrap; " +
                    "issues=${workspaceReport.issues.size}"
            }

            val report =
                StartupTrace.measure("Application.hierarchyAuthorityEstablishment") {
                    canonicalHierarchyAuthorityActivator.ensureEstablished()
                }
            if (report.performed) {
                Timber.i(
                    "Canonical Hierarchy authority established: outcome=%s occurrences=%d",
                    report.materialization?.outcome,
                    report.materialization?.occurrenceCount ?: 0,
                )
            }
        }

        // 2️⃣ ВСЕ ІНШЕ
        appScope.launch {
            runCatching {
                StartupTrace.measure("Application.featureToggles") {
                    settingsRepository.featureTogglesFlow.first()
                }
            }.onSuccess { toggles ->
                Timber.i("Feature toggles loaded")
                FeatureToggles.updateAll(toggles)
            }.onFailure {
                Timber.e(it, "Failed to load feature toggles")
            }
        }

        appScope.launch(Dispatchers.IO) {
            runCatching {
                StartupTrace.measure("Application.executionLogRepair") {
                    executionLogWorkspaceOwnershipBridge.repairUnresolved()
                }
            }.onSuccess { report ->
                if (report.assignedLogs > 0 || report.unresolvedContexts > 0) {
                    Timber.i(
                        "EXECUTION_LOG Workspace ownership repair: assigned=%d unresolvedContexts=%d",
                        report.assignedLogs,
                        report.unresolvedContexts,
                    )
                }
            }.onFailure {
                Timber.e(it, "Failed to repair EXECUTION_LOG Workspace ownership")
            }

            runCatching {
                StartupTrace.measure("Application.backlogCleanup") {
                    contextRepository.cleanupDanglingAndLegacyStructuralListItems()
                }
            }.onSuccess { cleanedCount ->
                if (cleanedCount > 0) {
                    Timber.i(
                        "Context backlog cleanup tombstoned %d dangling/legacy structural rows",
                        cleanedCount,
                    )
                }
            }.onFailure {
                Timber.e(it, "Failed to cleanup dangling/legacy structural backlog rows")
            }

            runCatching {
                StartupTrace.measure("Application.tagAssociationRepair") {
                    tagAssociationHandler.repairAllAssociations()
                }
            }.onFailure {
                Timber.e(it, "Failed to repair tag associations on startup")
            }
        }
        StartupTrace.mark("Application.onCreate.end")
    }
}
