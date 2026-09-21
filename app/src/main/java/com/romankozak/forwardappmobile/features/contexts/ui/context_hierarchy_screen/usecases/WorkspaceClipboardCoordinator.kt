package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextClipboardOperationUi
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

data class WorkspaceClipboardResult(
    val toast: String,
    val success: Boolean,
    val dismissDialog: Boolean = false,
)

@Singleton
class WorkspaceClipboardCoordinator
    @Inject
    constructor(
        private val canonicalWorkspaceRepository: CanonicalWorkspaceRepository,
        private val mainBeaconRepository: MainBeaconRepository,
    ) {
        private enum class Operation { COPY, CUT }

        private data class Payload(
            val workspaceIds: Set<String>,
            val operation: Operation,
            val token: String = UUID.randomUUID().toString(),
        )

        private val payload = MutableStateFlow<Payload?>(null)
        private val _uiState =
            MutableStateFlow(emptySet<String>() to null as ContextClipboardOperationUi?)

        val uiState: StateFlow<Pair<Set<String>, ContextClipboardOperationUi?>> =
            _uiState.asStateFlow()

        fun copyWorkspace(id: String) = copyWorkspaces(linkedSetOf(id))

        fun cutWorkspace(id: String) = cutWorkspaces(linkedSetOf(id))

        fun copyWorkspaces(ids: Set<String>): WorkspaceClipboardResult =
            setSources(ids, Operation.COPY)

        fun cutWorkspaces(ids: Set<String>): WorkspaceClipboardResult =
            setSources(ids, Operation.CUT)

        fun hasPayload(): Boolean = payload.value != null

        fun canPasteInto(targetId: String): Boolean {
            val current = payload.value ?: return false
            if (targetId.isBlank()) return false
            return targetId !in current.workspaceIds
        }

        suspend fun pasteIntoBeacon(beaconId: String): WorkspaceClipboardResult {
            val current =
                payload.value
                    ?: return WorkspaceClipboardResult(
                        "Буфер проєктів порожній",
                        false,
                        true,
                    )

            if (beaconId.isBlank()) {
                return WorkspaceClipboardResult(
                    "Неможливо вставити проєкт у цей орієнтир",
                    false,
                    true,
                )
            }

            return try {
                val affected =
                    when (current.operation) {
                        Operation.COPY ->
                            mainBeaconRepository.addRelatedContexts(
                                beaconId = beaconId,
                                contextIds = current.workspaceIds,
                            )

                        Operation.CUT ->
                            mainBeaconRepository.moveRelatedContextsToBeacon(
                                beaconId = beaconId,
                                contextIds = current.workspaceIds,
                            )
                    }

                when (current.operation) {
                    Operation.COPY ->
                        WorkspaceClipboardResult(
                            toast =
                                if (affected == 0) {
                                    "Посилання на проєкт уже є в цьому орієнтирі"
                                } else if (affected == 1) {
                                    "Проєкт додано до орієнтира як посилання"
                                } else {
                                    "Проєкти додано до орієнтира як посилання: $affected"
                                },
                            success = true,
                            dismissDialog = true,
                        )

                    Operation.CUT -> {
                        if (affected == 0) {
                            WorkspaceClipboardResult(
                                "Не вдалося перемістити посилання проєкту до орієнтира",
                                false,
                                true,
                            )
                        } else {
                            clearIfCurrent(current)
                            WorkspaceClipboardResult(
                                toast =
                                    if (affected == 1) {
                                        "Посилання проєкту переміщено до орієнтира"
                                    } else {
                                        "Посилання проєктів переміщено до орієнтира: $affected"
                                    },
                                success = true,
                                dismissDialog = true,
                            )
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                WorkspaceClipboardResult(
                    error.message ?: "Не вдалося вставити проєкти до орієнтира",
                    false,
                    true,
                )
            }
        }

        suspend fun pasteInto(targetId: String): WorkspaceClipboardResult {
            val current =
                payload.value
                    ?: return WorkspaceClipboardResult(
                        "Буфер проєктів порожній",
                        false,
                        true,
                    )

            if (!canPasteInto(targetId)) {
                return WorkspaceClipboardResult(
                    "Неможливо вставити проєкти сюди",
                    false,
                    true,
                )
            }

            return when (current.operation) {
                Operation.CUT ->
                    runCatching {
                        canonicalWorkspaceRepository.moveMany(
                            ids = current.workspaceIds,
                            newParentWorkspaceId = targetId,
                        )
                    }.fold(
                        onSuccess = { moved ->
                            clearIfCurrent(current)
                            WorkspaceClipboardResult(
                                if (moved.size == 1) {
                                    "Проєкт переміщено"
                                } else {
                                    "Проєкти переміщено: ${moved.size}"
                                },
                                true,
                                true,
                            )
                        },
                        onFailure = {
                            if (it is CancellationException) throw it
                            WorkspaceClipboardResult(
                                it.message ?: "Не вдалося перемістити проєкти",
                                false,
                                true,
                            )
                        },
                    )

                Operation.COPY ->
                    runCatching {
                        canonicalWorkspaceRepository.copyManyShallow(
                            ids = current.workspaceIds,
                            targetParentWorkspaceId = targetId,
                        )
                    }.fold(
                        onSuccess = { copied ->
                            WorkspaceClipboardResult(
                                if (copied.size == 1) {
                                    "Копію проєкту створено"
                                } else {
                                    "Копії проєктів створено: ${copied.size}"
                                },
                                true,
                                true,
                            )
                        },
                        onFailure = {
                            if (it is CancellationException) throw it
                            WorkspaceClipboardResult(
                                it.message ?: "Не вдалося скопіювати проєкти",
                                false,
                                true,
                            )
                        },
                    )
            }
        }

        private fun setSources(
            ids: Set<String>,
            operation: Operation,
        ): WorkspaceClipboardResult {
            val normalized =
                ids.asSequence()
                    .filter { it.isNotBlank() }
                    .toCollection(linkedSetOf())

            if (normalized.isEmpty()) {
                return WorkspaceClipboardResult(
                    "Немає проєктів для буфера",
                    false,
                )
            }

            payload.value = Payload(normalized, operation)
            syncUiState()

            return WorkspaceClipboardResult(
                toast =
                    when (operation) {
                        Operation.COPY ->
                            if (normalized.size == 1) {
                                "Проєкт скопійовано в буфер"
                            } else {
                                "Проєкти скопійовано в буфер: ${normalized.size}"
                            }

                        Operation.CUT ->
                            if (normalized.size == 1) {
                                "Проєкт вирізано"
                            } else {
                                "Проєкти вирізано: ${normalized.size}"
                            }
                    },
                success = true,
            )
        }

        private fun clearIfCurrent(expected: Payload) {
            if (payload.compareAndSet(expected, null)) {
                syncUiState()
            }
        }

        private fun syncUiState() {
            _uiState.value =
                when (val current = payload.value) {
                    null -> emptySet<String>() to null
                    else ->
                        current.workspaceIds to
                            when (current.operation) {
                                Operation.COPY -> ContextClipboardOperationUi.COPY
                                Operation.CUT -> ContextClipboardOperationUi.CUT
                            }
                }
        }
    }
