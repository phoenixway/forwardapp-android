package com.romankozak.forwardappmobile.features.semanticcreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.features.semantic.creatableOrientationKinds
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SemanticEntityCreationUiState(
    val name: String = "",
    val entityClass: SemanticEntityClass = SemanticEntityClass.ORIENTATION,
    val orientationKind: OrientationKind? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canCreate: Boolean
        get() =
            name.isNotBlank() &&
                !isSaving &&
                (entityClass == SemanticEntityClass.ASPECT || orientationKind in creatableOrientationKinds)
}

sealed interface SemanticEntityCreationEvent {
    data class Created(
        val id: String,
        val entityClass: SemanticEntityClass,
    ) : SemanticEntityCreationEvent
}

@HiltViewModel
class SemanticEntityCreationViewModel
    @Inject
    constructor(
        private val orientationRepository: CanonicalOrientationRepository,
        private val aspectRepository: CanonicalAspectRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SemanticEntityCreationUiState())
        val uiState = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<SemanticEntityCreationEvent>(extraBufferCapacity = 1)
        val events = _events.asSharedFlow()

        fun setName(name: String) {
            _uiState.update { it.copy(name = name, errorMessage = null) }
        }

        fun setEntityClass(entityClass: SemanticEntityClass) {
            _uiState.update {
                it.copy(
                    entityClass = entityClass,
                    orientationKind = if (entityClass == SemanticEntityClass.ORIENTATION) it.orientationKind else null,
                    errorMessage = null,
                )
            }
        }

        fun setOrientationKind(kind: OrientationKind) {
            require(kind in creatableOrientationKinds) { "Orientation kind is not available for new authoring" }
            _uiState.update { it.copy(orientationKind = kind, errorMessage = null) }
        }

        fun reset() {
            if (!_uiState.value.isSaving) {
                _uiState.value = SemanticEntityCreationUiState()
            }
        }

        fun create() {
            val request = _uiState.value
            if (!request.canCreate) return

            _uiState.value = request.copy(isSaving = true, errorMessage = null)
            viewModelScope.launch {
                runCatching {
                    val normalizedName = request.name.trim()
                    when (request.entityClass) {
                        SemanticEntityClass.ORIENTATION ->
                            orientationRepository.createOrientation(
                                kind = requireNotNull(request.orientationKind),
                                title = normalizedName,
                            )

                        SemanticEntityClass.ASPECT ->
                            aspectRepository.create(title = normalizedName)
                    }
                }.onSuccess { id ->
                    _events.emit(SemanticEntityCreationEvent.Created(id, request.entityClass))
                    _uiState.value = SemanticEntityCreationUiState()
                }.onFailure { failure ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = failure.message ?: "Не вдалося створити сутність",
                        )
                    }
                }
            }
        }
    }
