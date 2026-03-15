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

data class BacklogItemRepositories(
    val goalRepository: GoalRepository,
    val contextRepository: ContextRepository,
    val noteDocumentRepository: NoteDocumentRepository,
    val musicNoteRepository: MusicNoteRepository,
    val checklistRepository: ChecklistRepository,
    val noteRepository: LegacyNoteRepository,
    val listItemRepository: ListItemRepository,
    val dayManagementRepository: DayManagementRepository,
    val activityRepository: ActivityRepository,
    val contextTimeTrackingRepository: ContextTimeTrackingRepository,
)

class BacklogItemActions(
    private val repositories: BacklogItemRepositories,
) {
    suspend fun updateSubprojectCompleted(
        subproject: Context,
        completed: Boolean,
    ) {
        repositories.contextRepository.updateContext(subproject.copy(isCompleted = completed))
    }

    suspend fun updateProjectStatus(
        contextId: String,
        newStatus: String,
        statusText: String?,
    ) {
        repositories.contextRepository.updateContextStatus(contextId, newStatus, statusText)
    }

    suspend fun addCurrentProjectToDayPlan(contextId: String): String {
        if (contextId.isBlank()) return "Неможливо додати, проект не визначено"
        val day = repositories.dayManagementRepository.createOrUpdateDayPlan(System.currentTimeMillis())
        repositories.dayManagementRepository.addProjectToDayPlan(day.id, contextId)
        return "Проект додано до плану на сьогодні"
    }

    suspend fun deleteEverywhere(item: BacklogItemContent): String {
        return when (item) {
            is BacklogItemContent.GoalItem -> {
                repositories.goalRepository.deleteGoal(item.goal.id)
                "Ціль видалено"
            }

            is BacklogItemContent.ContextLinkItem -> {
                repositories.contextRepository.deleteContextsAndSubContexts(listOf(item.project))
                "Підконтекст видалено"
            }

            is BacklogItemContent.NoteDocumentItem -> {
                repositories.noteDocumentRepository.deleteDocument(item.document.id)
                "Документ видалено"
            }

            is BacklogItemContent.MusicNoteItem -> {
                repositories.musicNoteRepository.delete(item.musicNote.id)
                "Ноти видалено"
            }

            is BacklogItemContent.ChecklistItem -> {
                repositories.checklistRepository.deleteChecklist(item.checklist.id)
                "Чекліст видалено"
            }

            is BacklogItemContent.NoteItem -> {
                repositories.noteRepository.deleteNote(item.note.id)
                "Нотатку видалено"
            }

            is BacklogItemContent.LinkItem -> {
                repositories.contextRepository.deleteAttachmentEverywhere(item.backlogItem.id)
                repositories.listItemRepository.deleteItemByEntityId(item.link.id)
                "Посилання видалено"
            }
        }
    }

    suspend fun addItemToDailyPlan(item: BacklogItemContent): String {
        val day = repositories.dayManagementRepository.createOrUpdateDayPlan(System.currentTimeMillis())
        return when (item) {
            is BacklogItemContent.GoalItem -> {
                repositories.dayManagementRepository.addGoalToDayPlan(day.id, item.goal.id)
                "Додано в план дня"
            }

            is BacklogItemContent.ContextLinkItem -> {
                repositories.dayManagementRepository.addProjectToDayPlan(day.id, item.project.id)
                "Додано в план дня"
            }

            else -> "Цей тип елемента не можна додати в план дня"
        }
    }

    suspend fun startTracking(item: BacklogItemContent): String {
        return when (item) {
            is BacklogItemContent.GoalItem -> {
                repositories.activityRepository.startGoalActivity(item.goal.id)
                "Трекінг розпочато"
            }

            is BacklogItemContent.ContextLinkItem -> {
                repositories.activityRepository.startContextActivity(item.project.id)
                "Трекінг розпочато"
            }

            else -> "Для цього типу елемента трекінг недоступний"
        }
    }

    suspend fun recalculateTime(contextId: String): ContextTimeMetrics {
        val metrics = repositories.contextTimeTrackingRepository.calculateContextTimeMetrics(contextId)
        repositories.contextRepository.recalculateAndLogContextTime(contextId)
        return metrics
    }
}
