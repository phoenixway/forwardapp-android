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
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ContextScreenDataObserver(
    private val contextRepository: ContextRepository,
    private val listItemRepository: ListItemRepository,
    private val contextStructureRepository: ContextStructureRepository,
    private val contextLogRepository: ContextLogRepository,
    private val checklistRepository: ChecklistRepository,
    private val noteDocumentRepository: NoteDocumentRepository,
    private val musicNoteRepository: MusicNoteRepository,
    private val directionRepository: DirectionRepository,
    private val reminderRepository: ReminderRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val noteRepository: LegacyNoteRepository,
    private val goalRepository: GoalRepository,
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
                combine(
                    contextRepository.getContextByIdFlow(contextId),
                    listItemRepository.getItemsForContextStream(contextId),
                    contextStructureRepository.observeStructureOnly(contextId),
                    contextLogRepository.getContextLogsStream(contextId),
                    checklistRepository.getChecklistsForContext(contextId),
                    noteDocumentRepository.getDocumentsForContext(contextId),
                    musicNoteRepository.getMusicNotesForContext(contextId),
                    directionRepository.getDirectionItemsForContext(contextId),
                    contextRepository.getAllContextsFlow(),
                    contextRepository.getAttachmentsForContextStream(contextId),
                    listItemRepository.getAllEntitiesAsFlow(),
                    reminderRepository.getRemindersForEntityFlow(contextId),
                    recentItemsRepository.getRecentItemsForContextFlow(contextId),
                    noteRepository.getNotesForContext(contextId),
                    goalRepository.getGoalsByContextIdFlow(contextId),
                    contextRepository.getSubprojectsByParentIdFlow(contextId),
                ) { args: Array<Any?> ->
                    mapper.map(contextId = contextId, args = args)
                }
            }
        }
    }
}
