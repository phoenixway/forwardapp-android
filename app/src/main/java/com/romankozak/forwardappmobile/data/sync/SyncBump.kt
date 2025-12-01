package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef

private inline fun bumpVersion(version: Long) = if (version == Long.MAX_VALUE) version else version + 1

// Generic bump/soft-delete helpers for entities with (updatedAt, syncedAt, isDeleted, version)

fun Project.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Project.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Goal.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Goal.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ListItem.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ListItem.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ChecklistEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ChecklistEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ChecklistItemEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ChecklistItemEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun LegacyNoteEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun LegacyNoteEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun NoteDocumentEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun NoteDocumentEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun NoteDocumentItemEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun NoteDocumentItemEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun InboxRecord.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun InboxRecord.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ActivityRecord.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ActivityRecord.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun LinkItemEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun LinkItemEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ProjectExecutionLog.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ProjectExecutionLog.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ScriptEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ScriptEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun AttachmentEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun AttachmentEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ProjectAttachmentCrossRef.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ProjectAttachmentCrossRef.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayPlan.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayPlan.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayTask.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayTask.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DailyMetric.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DailyMetric.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Reminder.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Reminder.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))
