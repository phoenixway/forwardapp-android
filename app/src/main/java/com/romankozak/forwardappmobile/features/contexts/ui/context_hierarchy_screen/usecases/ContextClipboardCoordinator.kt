package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ContextParentPlanWriter
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyOccurrenceWriter
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
import com.romankozak.forwardappmobile.data.hierarchy.LegacyBeaconHierarchyTargetResolver
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyOccurrenceRef
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextClipboardOperationUi
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class ContextClipboardResult(
    val toast: String,
    val dismissDialog: Boolean = false,
)

@ViewModelScoped
class ContextClipboardCoordinator
    @Inject
    constructor(
        private val mainBeaconRepository: MainBeaconRepository,
        private val hierarchyOccurrenceCommandService: HierarchyOccurrenceCommandService,
        private val contextParentPlanWriter: CanonicalV2ContextParentPlanWriter,
        private val beaconOccurrenceWriter: CanonicalV2HierarchyOccurrenceWriter,
        private val beaconTargetResolver: LegacyBeaconHierarchyTargetResolver,
        private val orientationDao: OrientationDao,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private enum class ContextOperation {
            CUT,
            LINK,
        }

        private enum class BeaconOperation {
            COPY,
            CUT,
            LINK,
        }

        private data class Payload(
            val contextIds: Set<String>,
            val operation: ContextOperation,
            val occurrences: Map<String, HierarchyOccurrenceRef> = emptyMap(),
        )

        private data class BeaconPayload(
            val beaconId: String,
            val operation: BeaconOperation,
            val occurrence: HierarchyOccurrenceRef?,
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
                    operation = ContextOperation.LINK,
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
                    operation = ContextOperation.CUT,
                    occurrences = occurrence?.let { mapOf(contextId to it) }.orEmpty(),
                ),
            )
            return "Контекст вирізано"
        }

        fun copyBeacon(
            beaconId: String,
            occurrence: HierarchyOccurrenceRef? = null,
        ): String = setBeaconPayload(beaconId, occurrence, BeaconOperation.COPY)

        fun copyBeaconAsLink(
            beaconId: String,
            occurrence: HierarchyOccurrenceRef? = null,
        ): String = setBeaconPayload(beaconId, occurrence, BeaconOperation.LINK)

        fun cutBeacon(
            beaconId: String,
            occurrence: HierarchyOccurrenceRef? = null,
        ): String = setBeaconPayload(beaconId, occurrence, BeaconOperation.CUT)

        private fun setBeaconPayload(
            beaconId: String,
            occurrence: HierarchyOccurrenceRef?,
            operation: BeaconOperation,
        ): String {
            if (occurrence == null || occurrence.target.type != HierarchyTargetType.MANAGED_SUBJECT) {
                return "Неможливо вибрати орієнтир: відсутня точна occurrence"
            }
            payload.value = null
            syncUiState()
            beaconPayload.value = BeaconPayload(beaconId, operation, occurrence)
            syncBeaconUiState()
            return when (operation) {
                BeaconOperation.COPY -> "Головний орієнтир скопійовано"
                BeaconOperation.LINK -> "Посилання на головний орієнтир скопійовано"
                BeaconOperation.CUT -> "Головний орієнтир вирізано"
            }
        }

        suspend fun pasteBeaconIntoBeacon(
            targetBeaconId: String,
            destinationOccurrence: HierarchyOccurrenceRef? = null,
        ): ContextClipboardResult {
            val current = beaconPayload.value ?: return ContextClipboardResult("Буфер орієнтирів порожній")
            val destination = destinationOccurrence
                ?: return ContextClipboardResult("Відсутня точна occurrence цільового орієнтира")
            return pasteBeaconV2(
                current = current,
                destination = HierarchyClipboardDestination.BeaconOwner(targetBeaconId, destination),
                targetBeaconId = targetBeaconId,
            )
        }

        suspend fun pasteBeaconIntoGroup(groupId: String?): ContextClipboardResult {
            val current = beaconPayload.value ?: return ContextClipboardResult("Буфер орієнтирів порожній")
            return pasteBeaconV2(current, groupId = groupId)
        }

        private suspend fun pasteBeaconV2(
            current: BeaconPayload,
            destination: HierarchyClipboardDestination.BeaconOwner? = null,
            targetBeaconId: String? = null,
            groupId: String? = null,
        ): ContextClipboardResult {
            val source = current.occurrence
                ?: return ContextClipboardResult("Відсутня точна occurrence джерела")
            return try {
                withContext(ioDispatcher) {
                    val canonicalSource = requireNotNull(beaconTargetResolver.resolve(current.beaconId)) {
                        "Beacon source has no live canonical subject mapping"
                    }
                    require(source.target == canonicalSource) {
                        "Selected Beacon occurrence does not match its canonical target"
                    }
                    require(hierarchyOccurrenceCommandService.occurrence(source.placementId) == source) {
                        "Selected Beacon occurrence is stale or no longer exists"
                    }
                    val resolvedDestination = if (destination != null) {
                        val canonicalTarget = requireNotNull(
                            beaconTargetResolver.resolve(requireNotNull(targetBeaconId)),
                        ) { "Beacon destination has no live canonical subject mapping" }
                        require(destination.occurrence.target == canonicalTarget) {
                            "Destination Beacon occurrence does not match its canonical target"
                        }
                        require(
                            hierarchyOccurrenceCommandService.occurrence(
                                destination.occurrence.placementId,
                            ) == destination.occurrence,
                        ) { "Destination Beacon occurrence is stale or no longer exists" }
                        destination
                    } else {
                        val groupSubjectId = groupId?.let { legacyId ->
                            val mapping = requireNotNull(
                                orientationDao.getLegacyMapping(
                                    LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                                    legacyId,
                                ),
                            ) { "Group has no canonical subject mapping" }
                            require(
                                !mapping.isDeleted &&
                                    mapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                                    mapping.sourceId == legacyId &&
                                    mapping.sourceType == LegacyOrientationSourceType.MAIN_BEACON_GROUP.name &&
                                    orientationDao.getManagedSubject(mapping.subjectId)?.isDeleted == false,
                            ) { "Group canonical subject mapping is not live" }
                            mapping.subjectId
                        }
                        HierarchyClipboardDestination.Group(groupSubjectId)
                    }
                    val intent = HierarchyClipboardIntent(
                        operation = when (current.operation) {
                            BeaconOperation.COPY -> HierarchyClipboardOperation.COPY
                            BeaconOperation.CUT -> HierarchyClipboardOperation.CUT
                            BeaconOperation.LINK -> HierarchyClipboardOperation.LINK
                        },
                        sources = listOf(
                            HierarchyClipboardOccurrence(
                                occurrence = source,
                                sourceKind = HierarchyClipboardSourceKind.BEACON,
                                legacySourceId = current.beaconId,
                            ),
                        ),
                        destination = resolvedDestination,
                    )
                    val decision = if (destination != null) {
                        HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoBeacon(intent)
                    } else {
                        HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoGroup(intent)
                    }
                    val plan = when (decision) {
                        is HierarchyActionPlanDecision.Planned -> decision.plan
                        is HierarchyActionPlanDecision.Unsupported -> error(decision.reason)
                        is HierarchyActionPlanDecision.RequiresProductDecision -> error(decision.reason)
                    }
                    beaconOccurrenceWriter.executeBeaconPlan(plan)
                }
                if (current.operation == BeaconOperation.CUT) {
                    beaconPayload.value = null
                    syncBeaconUiState()
                }
                ContextClipboardResult("Операцію з appearance орієнтира виконано")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ContextClipboardResult(e.message ?: "Не вдалося змінити appearance орієнтира")
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
                if (current.operation == ContextOperation.CUT) {
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
                    ContextOperation.CUT -> HierarchyClipboardOperation.CUT
                    ContextOperation.LINK -> HierarchyClipboardOperation.LINK
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

            if (current.operation == ContextOperation.CUT) {
                clear()
            }

            return ContextClipboardResult(
                toast =
                    when (current.operation) {
                        ContextOperation.CUT ->
                            if (sources.size == 1) {
                                "Контекст переміщено"
                            } else {
                                "Контексти переміщено: ${sources.size}"
                            }
                        ContextOperation.LINK ->
                            if (sources.size == 1) {
                                "Додано посилання контексту"
                            } else {
                                "Додано посилання контекстів: ${sources.size}"
                            }
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
                        ContextOperation.CUT -> {
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
                        ContextOperation.LINK ->
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
            if (current.operation == ContextOperation.CUT) clear()
            return ContextClipboardResult(
                toast =
                    if (current.operation == ContextOperation.CUT) {
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
            if (current.operation != ContextOperation.CUT) {
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
                            ContextOperation.CUT -> ContextClipboardOperationUi.CUT
                            ContextOperation.LINK -> ContextClipboardOperationUi.COPY
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
