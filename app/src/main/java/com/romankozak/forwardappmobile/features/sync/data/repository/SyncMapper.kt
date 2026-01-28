package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextLogLevelValues
import com.romankozak.forwardappmobile.core.data.models.ContextStatusValues
import com.romankozak.forwardappmobile.core.data.models.ScoringStatusValues
import com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecord
import com.romankozak.forwardappmobile.features.attachments.data.models.*
import com.romankozak.forwardappmobile.features.contexts.data.models.*
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DailyMetric
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DayPlan
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DayTask
import com.romankozak.forwardappmobile.features.reminders.data.models.Reminder

/**
 * Відповідає за нормалізацію даних та обчислення метаданих для синхронізації.
 * Не містить залежностей від бази даних чи контексту Android.
 */
object SyncMapper {
    // --- Методи нормалізації ---

    fun normalizeGoal(goal: Goal): Goal {
        return goal.copy(
            tags = goal.tags ?: emptyList(),
            relatedLinks = goal.relatedLinks ?: emptyList(),
            scoringStatus = goal.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
            // Переконання, що всі числові поля мають значення за замовчуванням
            valueImportance = goal.valueImportance,
            valueImpact = goal.valueImpact,
            effort = goal.effort,
            cost = goal.cost,
            risk = goal.risk,
        )
    }

    fun normalizeProject(
        project: Context,
    ): Context {
        return project.copy(
            tags = project.tags ?: emptyList(),
            relatedLinks = project.relatedLinks ?: emptyList(),
            isContextManagementEnabled = project.isContextManagementEnabled ?: false,
            contextStatus = project.contextStatus ?: ContextStatusValues.NO_PLAN,
            contextStatusText = project.contextStatusText ?: "",
            contextLogLevel = project.contextLogLevel ?: ContextLogLevelValues.NORMAL,
            totalTimeSpentMinutes = project.totalTimeSpentMinutes ?: 0,
            scoringStatus = project.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED,
            // ViewMode залишаємо як є (не форсуємо BACKLOG при синхронізації)
            defaultViewModeName = project.defaultViewModeName,
        )
    }

    // --- Extension-функції для обчислення мітки часу оновлення (updatedTs) ---
    // Використовується для алгоритму LWW (Last-Write-Wins)

    fun Context.updatedTs(): Long = this.updatedAt ?: this.createdAt

    fun Goal.updatedTs(): Long = this.updatedAt ?: this.createdAt

    fun NoteDocumentEntity.updatedTs(): Long = this.updatedAt

    fun NoteDocumentItemEntity.updatedTs(): Long = this.updatedAt

    fun LegacyNoteEntity.updatedTs(): Long = this.updatedAt

    fun ChecklistEntity.updatedTs(): Long = this.updatedAt ?: this.version

    fun ChecklistItemEntity.updatedTs(): Long = this.updatedAt ?: this.version

    fun ActivityRecord.updatedTs(): Long = this.updatedAt ?: (this.endTime ?: this.startTime ?: this.createdAt)

    fun InboxRecord.updatedTs(): Long = this.updatedAt ?: this.createdAt

    fun LinkItemEntity.updatedTs(): Long = this.updatedAt ?: this.createdAt

    fun BacklogItem.updatedTs(): Long = this.updatedAt ?: this.version

    fun BacklogOrder.updatedTs(): Long = this.updatedAt ?: this.orderVersion

    fun ContextLog.updatedTs(): Long = this.updatedAt ?: this.timestamp

    fun ScriptEntity.updatedTs(): Long = this.updatedAt

    fun AttachmentEntity.updatedTs(): Long = this.updatedAt

    fun ContextAttachmentCrossRef.updatedTs(): Long = this.updatedAt ?: this.attachmentOrder.toLong()

    fun DayPlan.updatedTs(): Long = this.updatedAt ?: this.createdAt

    fun DayTask.updatedTs(): Long = this.updatedAt ?: this.createdAt

    fun DailyMetric.updatedTs(): Long = this.updatedAt ?: this.createdAt

    fun Reminder.updatedTs(): Long = this.updatedAt ?: this.creationTime
}
