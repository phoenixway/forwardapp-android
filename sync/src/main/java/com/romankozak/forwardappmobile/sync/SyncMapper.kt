package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLogLevelValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask

/**
 * Відповідає за нормалізацію даних та обчислення метаданих для синхронізації.
 * Не містить залежностей від бази даних чи контексту Android.
 */
object SyncMapper {
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

    fun MusicNoteEntity.updatedTs(): Long = this.updatedAt

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

    fun DirectionItemEntity.updatedTs(): Long = this.updatedAt ?: this.version


}
