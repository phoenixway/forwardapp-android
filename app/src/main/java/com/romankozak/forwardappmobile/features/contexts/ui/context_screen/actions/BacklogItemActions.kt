package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTimeMetrics
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextTimeTrackingRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository

class BacklogItemActions(
    private val goalRepository: GoalRepository,
    private val contextRepository: ContextRepository,
    private val noteDocumentRepository: NoteDocumentRepository,
    private val musicNoteRepository: MusicNoteRepository,
    private val checklistRepository: ChecklistRepository,
    private val noteRepository: LegacyNoteRepository,
    private val listItemRepository: ListItemRepository,
    private val dayManagementRepository: DayManagementRepository,
    private val activityRepository: ActivityRepository,
    private val contextTimeTrackingRepository: ContextTimeTrackingRepository,
) {
    suspend fun updateSubprojectCompleted(
        subproject: Context,
        completed: Boolean,
    ) {
        contextRepository.updateContext(subproject.copy(isCompleted = completed))
    }

    suspend fun updateProjectStatus(
        contextId: String,
        newStatus: String,
        statusText: String?,
    ) {
        contextRepository.updateContextStatus(contextId, newStatus, statusText)
    }

    suspend fun addCurrentProjectToDayPlan(contextId: String): String {
        if (contextId.isBlank()) return "Неможливо додати, проект не визначено"
        val day = dayManagementRepository.createOrUpdateDayPlan(System.currentTimeMillis())
        dayManagementRepository.addProjectToDayPlan(day.id, contextId)
        return "Проект додано до плану на сьогодні"
    }

    suspend fun deleteEverywhere(item: BacklogItemContent): String {
        return when (item) {
            is BacklogItemContent.GoalItem -> {
                goalRepository.deleteGoal(item.goal.id)
                "Ціль видалено"
            }

            is BacklogItemContent.ContextLinkItem -> {
                contextRepository.deleteContextsAndSubContexts(listOf(item.project))
                "Підконтекст видалено"
            }

            is BacklogItemContent.NoteDocumentItem -> {
                noteDocumentRepository.deleteDocument(item.document.id)
                "Документ видалено"
            }

            is BacklogItemContent.MusicNoteItem -> {
                musicNoteRepository.delete(item.musicNote.id)
                "Ноти видалено"
            }

            is BacklogItemContent.ChecklistItem -> {
                checklistRepository.deleteChecklist(item.checklist.id)
                "Чекліст видалено"
            }

            is BacklogItemContent.NoteItem -> {
                noteRepository.deleteNote(item.note.id)
                "Нотатку видалено"
            }

            is BacklogItemContent.LinkItem -> {
                contextRepository.deleteAttachmentEverywhere(item.backlogItem.id)
                listItemRepository.deleteItemByEntityId(item.link.id)
                "Посилання видалено"
            }
        }
    }

    suspend fun addItemToDailyPlan(item: BacklogItemContent): String {
        val day = dayManagementRepository.createOrUpdateDayPlan(System.currentTimeMillis())
        return when (item) {
            is BacklogItemContent.GoalItem -> {
                dayManagementRepository.addGoalToDayPlan(day.id, item.goal.id)
                "Додано в план дня"
            }

            is BacklogItemContent.ContextLinkItem -> {
                dayManagementRepository.addProjectToDayPlan(day.id, item.project.id)
                "Додано в план дня"
            }

            else -> "Цей тип елемента не можна додати в план дня"
        }
    }

    suspend fun startTracking(item: BacklogItemContent): String {
        return when (item) {
            is BacklogItemContent.GoalItem -> {
                activityRepository.startGoalActivity(item.goal.id)
                "Трекінг розпочато"
            }

            is BacklogItemContent.ContextLinkItem -> {
                activityRepository.startContextActivity(item.project.id)
                "Трекінг розпочато"
            }

            else -> "Для цього типу елемента трекінг недоступний"
        }
    }

    suspend fun recalculateTime(contextId: String): ContextTimeMetrics {
        val metrics = contextTimeTrackingRepository.calculateContextTimeMetrics(contextId)
        contextRepository.recalculateAndLogContextTime(contextId)
        return metrics
    }
}
