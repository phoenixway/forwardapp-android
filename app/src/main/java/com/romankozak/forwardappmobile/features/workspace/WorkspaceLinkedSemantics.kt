package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * One row per semantic subject, not per binding.
 *
 * An ambiguous, missing, or malformed target stays visible but cannot
 * be opened by guessing its identity.
 */
data class WorkspaceLinkedSemanticEntity(
    val ref: CanonicalEntityRef?,
    val title: String,
    val bindingTypes: List<String>,
    val isNavigable: Boolean,
) {
    fun navigationTarget(): NavTarget.SemanticEntityEditor? =
        if (
            isNavigable &&
            (ref is CanonicalEntityRef.Orientation || ref is CanonicalEntityRef.Aspect)
        ) {
            NavTarget.SemanticEntityEditor(ref.id)
        } else {
            null
        }
}

internal fun projectWorkspaceSemanticLinks(
    bindings: List<WorkspaceBindingEntity>,
    catalog: List<CanonicalEntitySummary>,
): List<WorkspaceLinkedSemanticEntity> {
    val candidatesBySubject =
        catalog
            .filter {
                it.ref is CanonicalEntityRef.Orientation ||
                    it.ref is CanonicalEntityRef.Aspect
            }
            .groupBy { it.ref.id }

    return bindings
        .filterNot { it.isDeleted }
        .groupBy { it.subjectId }
        .map { (subjectId, relations) ->
            // Fail closed on cross-class or duplicate-ID ambiguity.
            val summary = candidatesBySubject[subjectId].orEmpty().singleOrNull()
            val recognizedTypes =
                relations.map { relation ->
                    WorkspaceBindingType.entries.firstOrNull {
                        it.name == relation.bindingType
                    }
                }
            val validTypes = recognizedTypes.all { it != null }
            val labels =
                recognizedTypes.map { type ->
                    type?.name ?: "Некоректний тип зв’язку"
                }.distinct().sorted()

            WorkspaceLinkedSemanticEntity(
                ref = summary?.ref,
                title = summary?.title ?: "Семантична сутність недоступна",
                bindingTypes = labels,
                isNavigable = summary != null && subjectId.isNotBlank() && validTypes,
            )
        }
        .sortedWith(
            compareBy<WorkspaceLinkedSemanticEntity>(
                { !it.isNavigable },
                { it.title.lowercase(Locale.ROOT) },
                { it.ref?.entityClass?.name.orEmpty() },
                { it.ref?.id.orEmpty() },
                { it.bindingTypes.joinToString() },
            ),
        )
}

sealed interface WorkspaceLinkedSemanticsUiState {
    data object Loading : WorkspaceLinkedSemanticsUiState
    data class Ready(val links: List<WorkspaceLinkedSemanticEntity>) :
        WorkspaceLinkedSemanticsUiState
    data class Unavailable(val message: String) : WorkspaceLinkedSemanticsUiState
}

@HiltViewModel
class WorkspaceLinkedSemanticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    bindingRepository: CanonicalWorkspaceBindingRepository,
    catalogRepository: CanonicalEntityCatalogRepository,
) : ViewModel() {
    private val workspaceId = savedStateHandle.get<String>("workspaceId").orEmpty()

    val uiState: StateFlow<WorkspaceLinkedSemanticsUiState> =
        if (workspaceId.isBlank()) {
            MutableStateFlow(
                WorkspaceLinkedSemanticsUiState.Unavailable(
                    "Некоректний ідентифікатор Workspace",
                ),
            )
        } else {
            combine(
                bindingRepository.observeBindingsForWorkspace(workspaceId),
                catalogRepository.observeCatalog(),
            ) { bindings, catalog ->
                WorkspaceLinkedSemanticsUiState.Ready(
                    projectWorkspaceSemanticLinks(bindings, catalog),
                ) as WorkspaceLinkedSemanticsUiState
            }.catch {
                emit(
                    WorkspaceLinkedSemanticsUiState.Unavailable(
                        "Не вдалося завантажити семантичні зв’язки",
                    ),
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                WorkspaceLinkedSemanticsUiState.Loading,
            )
        }
}
