package com.romankozak.forwardappmobile.ui.screens.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectEditorState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@Inject
class MainScreenViewModel(
    private val projectRepository: ProjectRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _uiEventChannel = Channel<ProjectUiEvent>(Channel.BUFFERED)
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    init {
        observeProjects()
    }

    private fun observeProjects() {
        projectRepository
            .getAllProjects()
            .onEach { projects ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        projects = projects,
                        errorMessage = null,
                    )
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                }
                _uiEventChannel.trySend(
                    ProjectUiEvent.ShowToast("Не вдалося завантажити дані, перевірте лог")
                )
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: MainScreenEvent) {
        when (event) {
            MainScreenEvent.ShowCreateDialog -> {
                _uiState.update { it.copy(activeDialog = ProjectEditorState.Create()) }
            }
            is MainScreenEvent.ShowEditDialog -> showEditDialog(event.projectId)
            is MainScreenEvent.RequestDelete -> setProjectForDeletion(event.projectId)
            MainScreenEvent.HideDialog -> _uiState.update { it.copy(activeDialog = ProjectEditorState.Hidden) }
            MainScreenEvent.CancelDeletion -> _uiState.update { it.copy(pendingDeletion = null) }
            is MainScreenEvent.SubmitProject -> submitProject(event.name, event.description)
            MainScreenEvent.ConfirmDeletion -> deletePendingProject()
        }
    }

    private fun showEditDialog(projectId: String) {
        val project = _uiState.value.projects.firstOrNull { it.id == projectId } ?: return
        _uiState.update { it.copy(activeDialog = ProjectEditorState.Edit(project)) }
    }

    private fun setProjectForDeletion(projectId: String) {
        val project = _uiState.value.projects.firstOrNull { it.id == projectId } ?: return
        _uiState.update { it.copy(pendingDeletion = project) }
    }

    private fun submitProject(name: String, description: String) {
        val trimmedName = name.trim()
        val trimmedDescription = description.trim().ifBlank { null }
        if (trimmedName.isEmpty()) {
            viewModelScope.launch {
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Назва не може бути порожньою"))
            }
            return
        }

        val dialogState = _uiState.value.activeDialog
        if (dialogState is ProjectEditorState.Hidden) return

        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isActionInProgress = true) }
            try {
                when (dialogState) {
                    is ProjectEditorState.Create -> createProject(trimmedName, trimmedDescription, dialogState.parentId)
                    is ProjectEditorState.Edit -> updateProject(dialogState.project, trimmedName, trimmedDescription)
                    ProjectEditorState.Hidden -> return@launch
                }
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Проєкт збережено"))
            } catch (t: Throwable) {
                _uiEventChannel.send(
                    ProjectUiEvent.ShowToast(
                        "Помилка збереження: ${t.message ?: "невідома причина"}"
                    )
                )
            } finally {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        activeDialog = ProjectEditorState.Hidden,
                    )
                }
            }
        }
    }

    private suspend fun createProject(
        name: String,
        description: String?,
        parentId: String?,
    ) {
        val now = System.currentTimeMillis()
        val nextOrder = (_uiState.value.projects.maxOfOrNull { it.goalOrder } ?: -1L) + 1L
        val project =
            Project(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                parentId = parentId,
                createdAt = now,
                updatedAt = now,
                goalOrder = nextOrder,
            )
        projectRepository.upsertProject(project)
    }

    private suspend fun updateProject(
        existing: Project,
        name: String,
        description: String?,
    ) {
        val updated =
            existing.copy(
                name = name,
                description = description,
                updatedAt = System.currentTimeMillis(),
            )
        projectRepository.upsertProject(updated)
    }

    private fun deletePendingProject() {
        val project = _uiState.value.pendingDeletion ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isActionInProgress = true) }
            try {
                projectRepository.deleteProject(project.id)
                _uiEventChannel.send(ProjectUiEvent.ShowToast("Проєкт видалено"))
            } catch (t: Throwable) {
                _uiEventChannel.send(
                    ProjectUiEvent.ShowToast(
                        "Не вдалося видалити: ${t.message ?: "невідома причина"}"
                    )
                )
            } finally {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        pendingDeletion = null,
                    )
                }
            }
        }
    }
}
