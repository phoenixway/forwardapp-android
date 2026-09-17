package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.orientation.CanonicalContextMigrationRepository
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationCandidateReader
import com.romankozak.forwardappmobile.data.orientation.ContextMigrationTarget
import com.romankozak.forwardappmobile.data.orientation.classificationPreview
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextMigrationChoice
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state.DialogStateManager
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ContextMigrationCoordinator
    @Inject
    constructor(
        private val candidateReader: ContextMigrationCandidateReader,
        private val migrationRepository: CanonicalContextMigrationRepository,
        private val contextRepository: ContextRepository,
        private val dialogStateManager: DialogStateManager,
    ) {
        suspend fun start(projectId: String): Boolean {
            if (!canStartContextMigrationFromUi(projectId)) return false
            val context = contextRepository.getContextById(projectId) ?: return false
            dialogStateManager.showContextMigration(
                DialogState.ContextMigration(
                    projectId = context.id,
                    projectName = context.name,
                    preview = context.classificationPreview(),
                    aspectCandidates = candidateReader.existingAspectCandidates(),
                    orientationCandidates = candidateReader.existingOrientationCandidates(),
                ),
            )
            return true
        }

        fun selectChoice(choice: ContextMigrationChoice) =
            dialogStateManager.updateContextMigration {
                it.copy(selectedChoice = choice, confirmationRequested = false, errorMessage = null)
            }

        fun selectOrientationKind(kind: OrientationKind) =
            dialogStateManager.updateContextMigration {
                it.copy(selectedOrientationKind = kind, confirmationRequested = false, errorMessage = null)
            }

        fun selectExistingAspect(id: String) =
            dialogStateManager.updateContextMigration {
                it.copy(selectedExistingAspectId = id, confirmationRequested = false, errorMessage = null)
            }

        fun selectExistingOrientation(id: String) =
            dialogStateManager.updateContextMigration {
                it.copy(selectedExistingOrientationId = id, confirmationRequested = false, errorMessage = null)
            }

        fun requestConfirmation() =
            dialogStateManager.updateContextMigration { state ->
                val error = runCatching { state.materializeTarget() }.exceptionOrNull()?.message
                state.copy(confirmationRequested = error == null, errorMessage = error)
            }

        suspend fun execute(): Result<Unit> {
            val state = dialogStateManager.currentContextMigration() ?: return Result.failure(IllegalStateException("Migration dialog is not open"))
            if (!state.confirmationRequested) {
                return Result.failure(IllegalStateException("Потрібне явне підтвердження міграції"))
            }
            val target = runCatching { state.materializeTarget() }.getOrElse { error ->
                dialogStateManager.updateContextMigration { it.copy(errorMessage = error.message) }
                return Result.failure(error)
            }
            dialogStateManager.updateContextMigration { it.copy(isExecuting = true, errorMessage = null) }
            return runCatching { migrationRepository.migrateContext(state.projectId, target) }
                .map { Unit }
                .onFailure { error ->
                    dialogStateManager.updateContextMigration { it.copy(isExecuting = false, errorMessage = error.message ?: "Міграція не виконана") }
                }
        }
    }

internal fun DialogState.ContextMigration.materializeTarget(): ContextMigrationTarget =
    when (selectedChoice) {
        ContextMigrationChoice.NEW_ASPECT -> ContextMigrationTarget.NewAspectWithExistingWorkspace()
        ContextMigrationChoice.EXISTING_ASPECT -> ContextMigrationTarget.ExistingAspectWithExistingWorkspace(requireNotNull(selectedExistingAspectId) { "Оберіть існуючий аспект" })
        ContextMigrationChoice.NEW_ORIENTATION -> ContextMigrationTarget.NewOrientationWithExistingWorkspace(requireNotNull(selectedOrientationKind) { "Оберіть тип орієнтиру" })
        ContextMigrationChoice.EXISTING_ORIENTATION -> ContextMigrationTarget.ExistingOrientationWithExistingWorkspace(requireNotNull(selectedExistingOrientationId) { "Оберіть існуючий орієнтир" })
        ContextMigrationChoice.WORKSPACE_ONLY -> ContextMigrationTarget.WorkspaceOnly
        null -> throw IllegalArgumentException("Оберіть ціль міграції")
    }

internal fun canStartContextMigrationFromUi(projectId: String): Boolean =
    !SystemContexts.isSystem(ContextId(projectId))
