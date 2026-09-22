package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ContextParentPlanWriter
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyRead
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyActionPlanDecision
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyClipboardDestination
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyClipboardIntent
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyClipboardOccurrence
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyClipboardOperation
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyClipboardSourceKind
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyMixedDomainCommandSemantics
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceCommand
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceCommandService
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyOccurrenceRef
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
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import javax.inject.Inject

data class ContextClipboardResult(
    val toast: String,
    val dismissDialog: Boolean = false,
)

@ViewModelScoped
class ContextClipboardCoordinator
    @Inject
    constructor(
        private val mainBeaconRepository: MainBeaconRepository,
        private val workspaceClipboardCoordinator: WorkspaceClipboardCoordinator,
        private val hierarchyOccurrenceCommandService: HierarchyOccurrenceCommandService,
        private val contextParentPlanWriter: CanonicalV2ContextParentPlanWriter,
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
            val occurrences: Map<String, HierarchyOccurrenceRef> = emptyMap(),
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

        fun copyContextAsLink(
            contextId: String,
            occurrence: HierarchyOccurrenceRef? = null,
        ): String {
            setPayload(
                Payload(
                    contextIds = setOf(contextId),
                    operation = Operation.LINK,
                    occurrences = occurrence?.let { mapOf(contextId to it) }.orEmpty(),
                ),
            )
            return "Посилання на контекст скопійовано"
        }

        fun cutContext(
            contextId: String,
            occurrence: HierarchyOccurrenceRef? = null,
        ): String {
            setPayload(
                Payload(
                    contextIds = setOf(contextId),
                    operation = Operation.CUT,
                    occurrences = occurrence?.let { mapOf(contextId to it) }.orEmpty(),
                ),
            )
            return "Контекст вирізано"
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
            targetContextId: String,
            destinationOccurrence: HierarchyOccurrenceRef? = null,
            hierarchyRead: CanonicalV2ProductionHierarchyRead? = null,
        ): ContextClipboardResult {
            val current =
                payload.value
                    ?: return ContextClipboardResult(
                        "Буфер порожній",
                        dismissDialog = true,
                    )

            if (current.contextIds.isEmpty()) {
                clear()
                return ContextClipboardResult(
                    "Контекст у буфері більше не існує",
                    dismissDialog = true,
                )
            }

            if (current.operation == Operation.COPY) {
                withContext(ioDispatcher) {
                    current.contextIds.forEach { sourceId ->
                        workspaceClipboardCoordinator.copyWorkspaceInto(
                            sourceId = sourceId,
                            targetId = targetContextId,
                        )
                    }
                }
                return ContextClipboardResult(
                    toast =
                        if (current.contextIds.size == 1) {
                            "Контекст скопійовано в обраний контекст"
                        } else {
                            "Контексти скопійовано: ${current.contextIds.size}"
                        },
                    dismissDialog = true,
                )
            }

            val destination =
                destinationOccurrence
                    ?: return ContextClipboardResult(
                        "Неможливо вставити: відсутня identity occurrence цілі",
                        dismissDialog = true,
                    )

            if (destination.target.id != targetContextId) {
                return ContextClipboardResult(
                    "Неможливо вставити: цільова occurrence не відповідає контексту",
                    dismissDialog = true,
                )
            }

            val sources =
                current.contextIds.map { contextId ->
                    val occurrence =
                        current.occurrences[contextId]
                            ?: return ContextClipboardResult(
                                "Неможливо вставити: відсутня identity occurrence джерела",
                                dismissDialog = true,
                            )
                    HierarchyClipboardOccurrence(
                        occurrence = occurrence,
                        sourceKind = HierarchyClipboardSourceKind.CONTEXT_COMPATIBILITY,
                        legacySourceId = contextId,
                    )
                }

            val primaryOccurrencesByTarget =
                if (current.operation == Operation.CUT) {
                    val read =
                        hierarchyRead
                            ?: return ContextClipboardResult(
                                "Неможливо перемістити: canonical V2 hierarchy read недоступний",
                                dismissDialog = true,
                            )
                    buildMap {
                        sources.forEach { source ->
                            val primary =
                                read.workspaceOccurrences(source.occurrence.target.id)
                                    .firstOrNull { it.placementKind == PlacementKind.PRIMARY }
                                    ?.toHierarchyOccurrenceRef()
                                    ?: return ContextClipboardResult(
                                        "Неможливо перемістити: PRIMARY occurrence не знайдено",
                                        dismissDialog = true,
                                    )
                            put(source.occurrence.target, primary)
                        }
                    }
                } else {
                    emptyMap()
                }

            val operation =
                when (current.operation) {
                    Operation.COPY -> error("COPY handled before V2 context planning")
                    Operation.CUT -> HierarchyClipboardOperation.CUT
                    Operation.LINK -> HierarchyClipboardOperation.LINK
                }

            val decision =
                HierarchyMixedDomainCommandSemantics.planContextPasteIntoParent(
                    intent =
                        HierarchyClipboardIntent(
                            operation = operation,
                            sources = sources,
                            destination =
                                HierarchyClipboardDestination.ParentOccurrence(
                                    parentPlacementId = destination.placementId,
                                ),
                        ),
                    primaryOccurrencesByTarget = primaryOccurrencesByTarget,
                    destinationParentTarget = destination.target,
                )

            val plan =
                when (decision) {
                    is HierarchyActionPlanDecision.Planned -> decision.plan
                    is HierarchyActionPlanDecision.Unsupported ->
                        return ContextClipboardResult(
                            decision.reason,
                            dismissDialog = true,
                        )
                    is HierarchyActionPlanDecision.RequiresProductDecision ->
                        return ContextClipboardResult(
                            decision.reason,
                            dismissDialog = true,
                        )
                }

            withContext(ioDispatcher) {
                contextParentPlanWriter.execute(plan)
            }

            if (current.operation == Operation.CUT) {
                clear()
            }

            return ContextClipboardResult(
                toast =
                    when (current.operation) {
                        Operation.CUT ->
                            if (sources.size == 1) {
                                "Контекст переміщено"
                            } else {
                                "Контексти переміщено: ${sources.size}"
                            }
                        Operation.LINK ->
                            if (sources.size == 1) {
                                "Додано посилання контексту"
                            } else {
                                "Додано посилання контекстів: ${sources.size}"
                            }
                        Operation.COPY -> error("COPY handled before V2 context planning")
                    },
                dismissDialog = true,
            )
        }

        suspend fun pasteIntoBeacon(
            beaconNodeId: String,
            orientationHierarchy: List<OrientationHierarchyItem>,
        ): ContextClipboardResult {
            val current = payload.value ?: return ContextClipboardResult("Буфер порожній")
            val beaconNode =
                orientationHierarchy
                    .firstOrNull { it.node.id == beaconNodeId }
                    ?.node
            if (beaconNode !is OrientationHierarchyNode.Beacon) {
                return ContextClipboardResult("Вставка доступна тільки в головний орієнтир")
            }

            val contextIds = current.contextIds
            if (contextIds.isEmpty()) {
                clear()
                return ContextClipboardResult("Контекст у буфері більше не існує")
            }

            val addedCount =
                withContext(ioDispatcher) {
                    when (current.operation) {
                        Operation.CUT -> {
                            val occurrences =
                                contextIds.map { contextId ->
                                    current.occurrences[contextId]
                                        ?: return@withContext null
                                }
                            occurrences.forEach { occurrence ->
                                hierarchyOccurrenceCommandService.removeOccurrence(
                                    HierarchyOccurrenceCommand.RemoveOccurrence(
                                        placementId = occurrence.placementId,
                                    ),
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

            if (addedCount == null) {
                return ContextClipboardResult(
                    "Неможливо перемістити: відсутня identity occurrence",
                )
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

        suspend fun pasteIntoNoBeacon(): ContextClipboardResult {
            val current = payload.value ?: return ContextClipboardResult("Буфер порожній")
            if (current.operation != Operation.CUT) {
                return ContextClipboardResult("У No beacon можна лише перемістити контекст")
            }

            val contextIds = current.contextIds
            if (contextIds.isEmpty()) {
                clear()
                return ContextClipboardResult("Контекст у буфері більше не існує")
            }

            val occurrences =
                contextIds.map { contextId ->
                    current.occurrences[contextId]
                        ?: return ContextClipboardResult(
                            "Неможливо перемістити: відсутня identity occurrence",
                        )
                }

            withContext(ioDispatcher) {
                occurrences.forEach { occurrence ->
                    hierarchyOccurrenceCommandService.removeOccurrence(
                        HierarchyOccurrenceCommand.RemoveOccurrence(
                            placementId = occurrence.placementId,
                        ),
                    )
                }
                mainBeaconRepository.removeContextsFromAllBeacons(contextIds)
            }

            clear()
            return ContextClipboardResult(
                toast =
                    if (contextIds.size == 1) {
                        "Контекст переміщено в No beacon"
                    } else {
                        "Контексти переміщено в No beacon: ${contextIds.size}"
                    },
            )
        }

        suspend fun addContextAppearance(
            parentOccurrence: HierarchyOccurrenceRef?,
        ): ContextClipboardResult {
            val current = payload.value ?: return ContextClipboardResult("Буфер порожній", dismissDialog = true)
            val destination =
                parentOccurrence
                    ?: return ContextClipboardResult(
                        "Неможливо додати появу: відсутня identity occurrence цілі",
                        dismissDialog = true,
                    )
            val sourceOccurrences =
                current.contextIds.map { contextId ->
                    current.occurrences[contextId]
                        ?: return ContextClipboardResult(
                            "Неможливо додати появу: відсутня identity occurrence джерела",
                            dismissDialog = true,
                        )
                }

            withContext(ioDispatcher) {
                sourceOccurrences.forEach { source ->
                    hierarchyOccurrenceCommandService.createAppearance(
                        HierarchyOccurrenceCommand.CreateAppearance(
                            target = source.target,
                            parentPlacementId = destination.placementId,
                            placementKind = PlacementKind.LINK,
                        ),
                    )
                }
            }

            return ContextClipboardResult(
                toast =
                    if (sourceOccurrences.size == 1) {
                        "Додано появу контексту"
                    } else {
                        "Додано появи контекстів: ${sourceOccurrences.size}"
                    },
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
