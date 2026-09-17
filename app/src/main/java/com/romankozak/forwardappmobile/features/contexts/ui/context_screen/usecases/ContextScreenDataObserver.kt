package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.DirectionRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalInboxDirectionAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalRemainingCapabilityLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemContextCanonicalBacklogLifecycleAccess
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ContextScreenDataObserver(
    private val dependencies: ContextScreenDataObserverDependencies,
    private val mapper: ContextScreenDataMapper,
) {
    fun observe(
        contextIdFlow: Flow<String>,
        refreshTriggerFlow: Flow<Int>,
    ): Flow<ContextData> {
        return combine(contextIdFlow, refreshTriggerFlow.distinctUntilChanged()) { contextId, _ ->
            contextId
        }.flatMapLatest { contextId ->
            if (contextId.isBlank()) {
                flowOf(ContextData.Empty)
            } else {
                val rawContexts = dependencies.contextRepository.getAllContextsFlow()
                val presentationUniverse =
                    dependencies.systemWorkspacePresentationContextProjector.observePresentationUniverse(rawContexts)
                val currentContextReadModel =
                    combine(presentationUniverse, rawContexts) { presentations, contexts ->
                        contextScreenReadModel(
                            contextId = contextId,
                            presentations = presentations,
                            contexts = contexts,
                        )
                    }
                combine(
                    currentContextReadModel,
                    dependencies.listItemRepository.getItemsForContextStream(contextId),
                    dependencies.contextStructureRepository.observeStructureOnly(contextId),
                    dependencies.contextLogRepository.getContextLogsStream(contextId),
                    dependencies.checklistRepository.getChecklistsForContext(contextId),
                    dependencies.noteDocumentRepository.getDocumentsForContext(contextId),
                    dependencies.musicNoteRepository.getMusicNotesForContext(contextId),
                    dependencies.directionRepository.getDirectionItemsForContext(contextId),
                    dependencies.contextRepository.getAttachmentsForContextStream(contextId),
                    dependencies.listItemRepository.getAllEntitiesAsFlow(),
                    dependencies.reminderRepository.getAllReminders(),
                    dependencies.recentItemsRepository.getRecentItems(RECENT_ITEMS_LIMIT),
                    dependencies.noteRepository.getNotesForContext(contextId),
                    dependencies.goalRepository.getGoalsByContextIdFlow(contextId),
                    dependencies.orientationDao.observeWorkspaceCapabilities(contextId),
                    dependencies.systemInboxDirectionAccess.observeState(contextId),
                    dependencies.systemRemainingCapabilityAccess.observeState(contextId),
                    dependencies.systemBacklogLifecycleAccess.observeState(contextId),
                ) { args: Array<Any?> ->
                    mapper.map(
                        contextId = contextId,
                        snapshot = ContextScreenDataSnapshot.fromArgs(args),
                    )
                }
            }
        }
    }
}

private const val RECENT_ITEMS_LIMIT = 100

data class ContextScreenDataObserverDependencies(
    val contextRepository: ContextRepository,
    val listItemRepository: ListItemRepository,
    val contextStructureRepository: ContextStructureRepository,
    val contextLogRepository: ContextLogRepository,
    val checklistRepository: ChecklistRepository,
    val noteDocumentRepository: NoteDocumentRepository,
    val musicNoteRepository: MusicNoteRepository,
    val directionRepository: DirectionRepository,
    val reminderRepository: ReminderRepository,
    val recentItemsRepository: RecentItemsRepository,
    val noteRepository: LegacyNoteRepository,
    val goalRepository: GoalRepository,
    val orientationDao: OrientationDao,
    val systemInboxDirectionAccess: SystemContextCanonicalInboxDirectionAccess,
    val systemRemainingCapabilityAccess: SystemContextCanonicalRemainingCapabilityLifecycleAccess,
    val systemBacklogLifecycleAccess: SystemContextCanonicalBacklogLifecycleAccess,
    val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
)
