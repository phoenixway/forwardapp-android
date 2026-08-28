package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextClipboardOperationUi
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class ContextClipboardResult(
    val toast: String,
    val dismissDialog: Boolean = false,
)

@ViewModelScoped
class ContextClipboardCoordinator
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val mainBeaconRepository: MainBeaconRepository,
        private val contextActionsUseCase: ContextActionsUseCase,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private enum class Operation {
            COPY,
            CUT,
            LINK,
        }

        private data class Payload(
            val contextIds: Set<String>,
            val operation: Operation,
            val sourceParentIds: Map<String, String?> = emptyMap(),
        )

        private data class BeaconPayload(
            val beaconId: String,
            val operation: Operation,
        )

        private val payload = MutableStateFlow<Payload?>(null)
        private val beaconPayload = MutableStateFlow<BeaconPayload?>(null)
        private val _hasBeaconPayload = MutableStateFlow(false)
        private val _uiState = MutableStateFlow(emptySet<String>() to null as ContextClipboardOperationUi?)

        val uiState: StateFlow<Pair<Set<String>, ContextClipboardOperationUi?>> = _uiState.asStateFlow()
        val hasBeaconPayload: StateFlow<Boolean> = _hasBeaconPayload.asStateFlow()

        fun retainExistingContextIds(existingIds: Set<String>) {
            payload.update { current ->
                current
                    ?.copy(contextIds = current.contextIds.intersect(existingIds))
                    ?.takeIf { it.contextIds.isNotEmpty() }
            }
            syncUiState()
        }

        fun canPasteInto(
            targetContextId: String,
            allProjects: List<Context>,
        ): Boolean {
            val current = payload.value ?: return false
            return canPasteInto(targetContextId = targetContextId, allProjects = allProjects, payload = current)
        }

        fun copyContext(contextId: String): String {
            setPayload(Payload(contextIds = setOf(contextId), operation = Operation.COPY))
            return "Контекст скопійовано"
        }

        fun copyContextAsLink(contextId: String): String {
            setPayload(Payload(contextIds = setOf(contextId), operation = Operation.LINK))
            return "Посилання на контекст скопійовано"
        }

        fun cutContext(
            contextId: String,
            sourceParentId: String? = null,
        ): String {
            setPayload(
                Payload(
                    contextIds = setOf(contextId),
                    operation = Operation.CUT,
                    sourceParentIds = mapOf(contextId to sourceParentId),
                ),
            )
            return "Контекст вирізано"
        }

        fun copyContexts(contextIds: Set<String>): ContextClipboardResult? {
            if (contextIds.isEmpty()) return null
            setPayload(Payload(contextIds = contextIds, operation = Operation.COPY))
            return ContextClipboardResult(toast = "Контексти скопійовано: ${contextIds.size}")
        }

        fun cutContexts(
            contextIds: Set<String>,
            sourceParentIds: Map<String, String?> = emptyMap(),
        ): ContextClipboardResult? {
            if (contextIds.isEmpty()) return null
            setPayload(
                Payload(
                    contextIds = contextIds,
                    operation = Operation.CUT,
                    sourceParentIds = sourceParentIds.filterKeys { it in contextIds },
                ),
            )
            return ContextClipboardResult(toast = "Контексти вирізано: ${contextIds.size}")
        }

        fun copyBeacon(beaconId: String): String {
            payload.value = null
            syncUiState()
            beaconPayload.value = BeaconPayload(beaconId = beaconId, operation = Operation.COPY)
            syncBeaconUiState()
            return "Головний орієнтир скопійовано"
        }

        fun copyBeaconAsLink(beaconId: String): String {
            payload.value = null
            syncUiState()
            beaconPayload.value = BeaconPayload(beaconId = beaconId, operation = Operation.LINK)
            syncBeaconUiState()
            return "Посилання на головний орієнтир скопійовано"
        }

        fun cutBeacon(beaconId: String): String {
            payload.value = null
            syncUiState()
            beaconPayload.value = BeaconPayload(beaconId = beaconId, operation = Operation.CUT)
            syncBeaconUiState()
            return "Головний орієнтир вирізано"
        }

        suspend fun pasteBeaconIntoBeacon(targetBeaconId: String): ContextClipboardResult {
            val current = beaconPayload.value ?: return ContextClipboardResult("Буфер орієнтирів порожній")
            return when (current.operation) {
                Operation.CUT -> {
                    val moved =
                        withContext(ioDispatcher) {
                            mainBeaconRepository.moveBeaconToParent(
                                beaconId = current.beaconId,
                                parentBeaconId = targetBeaconId,
                            )
                    }
                    if (moved) {
                        beaconPayload.value = null
                        syncBeaconUiState()
                        ContextClipboardResult("Головний орієнтир переміщено")
                    } else {
                        ContextClipboardResult("Неможливо вставити головний орієнтир сюди")
                    }
                }
                Operation.COPY,
                Operation.LINK -> {
                    val linked =
                        withContext(ioDispatcher) {
                            mainBeaconRepository.addBeaconParentLink(
                                childBeaconId = current.beaconId,
                                parentBeaconId = targetBeaconId,
                            )
                        }
                    ContextClipboardResult(
                        if (linked) {
                            "Посилання на головний орієнтир додано"
                        } else {
                            "Орієнтир уже є тут, більше не існує або створив би цикл"
                        },
                    )
                }
            }
        }

        suspend fun pasteBeaconIntoGroup(groupId: String?): ContextClipboardResult {
            val current = beaconPayload.value ?: return ContextClipboardResult("Буфер орієнтирів порожній")
            return when (current.operation) {
                Operation.CUT -> {
                    withContext(ioDispatcher) {
                        mainBeaconRepository.moveBeaconToGroup(
                            beaconId = current.beaconId,
                            groupId = groupId,
                        )
                    }
                    beaconPayload.value = null
                    syncBeaconUiState()
                    ContextClipboardResult("Головний орієнтир переміщено в групу")
                }
                Operation.COPY,
                Operation.LINK -> {
                    val added =
                        withContext(ioDispatcher) {
                            mainBeaconRepository.addBeaconToGroup(current.beaconId, groupId)
                        }
                    ContextClipboardResult(
                        if (added) {
                            "Головний орієнтир додано в групу"
                        } else {
                            "Орієнтир уже є в цій групі або більше не існує"
                        },
                    )
                }
            }
        }

        suspend fun pasteIntoContext(
            targetContext: Context,
            allProjects: List<Context>,
        ): ContextClipboardResult {
            val current = payload.value ?: return ContextClipboardResult("Буфер порожній", dismissDialog = true)
            val sources = resolveClipboardContexts(allProjects, current)
            if (sources.isEmpty()) {
                clear()
                return ContextClipboardResult("Контекст у буфері більше не існує", dismissDialog = true)
            }
            if (!canPasteInto(targetContext.id, allProjects, current)) {
                return ContextClipboardResult("Неможливо вставити в цей контекст", dismissDialog = true)
            }

            return when (current.operation) {
                Operation.CUT -> {
                    withContext(ioDispatcher) {
                        sources.forEach { source ->
                            current.sourceParentIds[source.id]?.let { sourceParentId ->
                                if (sourceParentId != source.parentId && sourceParentId != targetContext.id) {
                                    contextActionsUseCase.removeAdditionalParentLink(
                                        parentContextId = sourceParentId,
                                        childContextId = source.id,
                                    )
                                }
                            }
                            contextRepository.moveContext(
                                contextToMove = source,
                                newParentId = targetContext.id,
                                allowSystemMoves = true,
                            )
                        }
                    }
                    clear()
                    ContextClipboardResult(
                        toast = if (sources.size == 1) "Контекст переміщено" else "Контексти переміщено: ${sources.size}",
                        dismissDialog = true,
                    )
                }

                Operation.COPY -> {
                    withContext(ioDispatcher) {
                        val siblingNames =
                            allProjects
                                .filter { it.parentId == targetContext.id }
                                .mapTo(mutableSetOf()) { it.name }
                        sources.forEach { source ->
                            val copiedName =
                                generateCopiedContextName(
                                    baseName = source.name,
                                    existingSiblingNames = siblingNames,
                                )
                            siblingNames += copiedName
                            contextRepository.createContextWithId(
                                id = UUID.randomUUID().toString(),
                                name = copiedName,
                                parentId = targetContext.id,
                                roleCode = source.roleCode,
                            )
                        }
                    }
                    ContextClipboardResult(
                        toast =
                            if (sources.size == 1) {
                                "Контекст скопійовано в обраний контекст"
                            } else {
                                "Контексти скопійовано: ${sources.size}"
                            },
                        dismissDialog = true,
                    )
                }

                Operation.LINK -> {
                    val addedCount =
                        contextActionsUseCase.addAdditionalParentLinks(
                            parentContextId = targetContext.id,
                            childContextIds = sources.mapTo(linkedSetOf()) { it.id },
                            allProjects = allProjects,
                        )
                    ContextClipboardResult(
                        toast = if (addedCount == 0) "Нові посилання не додано" else "Додано посилання контекстів: $addedCount",
                        dismissDialog = true,
                    )
                }
            }
        }

        suspend fun pasteIntoBeacon(
            beaconNodeId: String,
            orientationHierarchy: List<OrientationHierarchyItem>,
            allProjects: List<Context>,
        ): ContextClipboardResult {
            val current = payload.value ?: return ContextClipboardResult("Буфер порожній")
            val beaconNode =
                orientationHierarchy
                    .firstOrNull { it.node.id == beaconNodeId }
                    ?.node
            if (beaconNode !is OrientationHierarchyNode.Beacon) {
                return ContextClipboardResult("Вставка доступна тільки в головний орієнтир")
            }

            val sources = resolveClipboardContexts(allProjects, current)
            if (sources.isEmpty()) {
                clear()
                return ContextClipboardResult("Контекст у буфері більше не існує")
            }

            val contextIds = sources.mapTo(linkedSetOf()) { it.id }
            val addedCount =
                withContext(ioDispatcher) {
                    when (current.operation) {
                        Operation.CUT -> {
                            sources.forEach { source ->
                                detachContextFromDisplayedLocation(
                                    source = source,
                                    sourceParentId = current.sourceParentIds[source.id],
                                )
                            }
                            mainBeaconRepository.moveRelatedContextsToBeacon(
                                beaconId = beaconNode.id,
                                contextIds = contextIds,
                            )
                        }
                        Operation.COPY,
                        Operation.LINK ->
                            mainBeaconRepository.addRelatedContexts(
                                beaconId = beaconNode.id,
                                contextIds = contextIds,
                            )
                    }
                }
            if (current.operation == Operation.CUT) clear()
            return ContextClipboardResult(
                toast =
                    if (current.operation == Operation.CUT) {
                        if (addedCount == 0) {
                            "Контекст не вдалося перемістити до головного орієнтира"
                        } else {
                            "Контекст переміщено до головного орієнтира"
                        }
                    } else if (addedCount == 0) {
                        "Нові зв'язки з головним орієнтиром не додано"
                    } else {
                        "Додано контексти до головного орієнтира: $addedCount"
                    },
            )
        }

        suspend fun pasteIntoNoBeacon(
            allProjects: List<Context>,
        ): ContextClipboardResult {
            val current = payload.value ?: return ContextClipboardResult("Буфер порожній")
            if (current.operation != Operation.CUT) {
                return ContextClipboardResult("У No beacon можна лише перемістити контекст")
            }

            val sources = resolveClipboardContexts(allProjects, current)
            if (sources.isEmpty()) {
                clear()
                return ContextClipboardResult("Контекст у буфері більше не існує")
            }

            val contextIds = sources.mapTo(linkedSetOf()) { it.id }
            withContext(ioDispatcher) {
                sources.forEach { source ->
                    detachContextFromDisplayedLocation(
                        source = source,
                        sourceParentId = current.sourceParentIds[source.id],
                    )
                }
                mainBeaconRepository.removeContextsFromAllBeacons(contextIds)
            }

            clear()
            return ContextClipboardResult(
                toast =
                    if (sources.size == 1) {
                        "Контекст переміщено в No beacon"
                    } else {
                        "Контексти переміщено в No beacon: ${sources.size}"
                    },
            )
        }

        suspend fun addContextAppearance(
            parentContext: Context,
            allProjects: List<Context>,
        ): ContextClipboardResult {
            val current = payload.value ?: return ContextClipboardResult("Буфер порожній", dismissDialog = true)
            val sources = resolveClipboardContexts(allProjects, current)
            if (sources.isEmpty()) {
                clear()
                return ContextClipboardResult("Контекст у буфері більше не існує", dismissDialog = true)
            }

            val addedCount =
                contextActionsUseCase.addAdditionalParentLinks(
                    parentContextId = parentContext.id,
                    childContextIds = sources.mapTo(linkedSetOf()) { it.id },
                    allProjects = allProjects,
                )
            return ContextClipboardResult(
                toast = if (addedCount == 0) "Нові появи не додано" else "Додано появи контекстів: $addedCount",
                dismissDialog = true,
            )
        }

        private fun setPayload(newPayload: Payload) {
            beaconPayload.value = null
            syncBeaconUiState()
            payload.value = newPayload
            syncUiState()
        }

        private fun clear() {
            payload.value = null
            syncUiState()
        }

        private fun syncUiState() {
            _uiState.value =
                payload.value?.let { current ->
                    current.contextIds to
                        when (current.operation) {
                            Operation.COPY -> ContextClipboardOperationUi.COPY
                            Operation.CUT -> ContextClipboardOperationUi.CUT
                            Operation.LINK -> ContextClipboardOperationUi.COPY
                        }
                } ?: (emptySet<String>() to null)
        }

        private fun syncBeaconUiState() {
            _hasBeaconPayload.value = beaconPayload.value != null
        }

        private fun canPasteInto(
            targetContextId: String,
            allProjects: List<Context>,
            payload: Payload,
        ): Boolean {
            if (targetContextId.isBlank()) return false
            val target = allProjects.firstOrNull { it.id == targetContextId } ?: return false
            val sources = resolveClipboardContexts(allProjects, payload)
            if (sources.isEmpty()) return false

            return when (payload.operation) {
                Operation.COPY,
                Operation.LINK -> sources.none { it.id == target.id }
                Operation.CUT ->
                    sources.none { it.id == target.id } &&
                        sources.none { source ->
                            isDescendantOrSelf(
                                candidateDescendantId = target.id,
                                ancestorId = source.id,
                                allProjects = allProjects,
                            )
                        }
            }
        }

        private fun resolveClipboardContexts(
            allProjects: List<Context>,
            payload: Payload,
        ): List<Context> {
            val contextsById = allProjects.associateBy { it.id }
            val resolvedContexts =
                payload.contextIds
                    .mapNotNull(contextsById::get)
            if (payload.operation == Operation.CUT) {
                return resolvedContexts
            }
            return resolvedContexts
                .filterNot { candidate ->
                    payload.contextIds.any { otherId ->
                        otherId != candidate.id &&
                            isDescendantOrSelf(
                                candidateDescendantId = candidate.id,
                                ancestorId = otherId,
                                allProjects = allProjects,
                            )
                    }
                }
        }

        private suspend fun detachContextFromDisplayedLocation(
            source: Context,
            sourceParentId: String?,
        ) {
            if (sourceParentId != null && sourceParentId != source.parentId) {
                contextActionsUseCase.removeAdditionalParentLink(
                    parentContextId = sourceParentId,
                    childContextId = source.id,
                )
            }
            if (source.parentId != null) {
                contextRepository.moveContext(
                    contextToMove = source,
                    newParentId = null,
                    allowSystemMoves = true,
                )
            }
        }

        private fun isDescendantOrSelf(
            candidateDescendantId: String,
            ancestorId: String,
            allProjects: List<Context>,
        ): Boolean {
            if (candidateDescendantId == ancestorId) return true
            val parentById = allProjects.associate { it.id to it.parentId }
            var currentParent = parentById[candidateDescendantId]
            while (!currentParent.isNullOrBlank()) {
                if (currentParent == ancestorId) return true
                currentParent = parentById[currentParent]
            }
            return false
        }

        private fun generateCopiedContextName(
            baseName: String,
            existingSiblingNames: Set<String>,
        ): String {
            val firstCandidate = "$baseName (копія)"
            if (firstCandidate !in existingSiblingNames) return firstCandidate
            var index = 2
            while (true) {
                val candidate = "$baseName (копія $index)"
                if (candidate !in existingSiblingNames) return candidate
                index += 1
            }
        }
    }
