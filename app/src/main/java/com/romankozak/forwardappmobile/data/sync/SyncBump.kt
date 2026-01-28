package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecord
import com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ScriptEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.features.contexts.data.models.InboxRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DailyMetric
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DayPlan
import com.romankozak.forwardappmobile.features.daymanagement.data.models.DayTask
import com.romankozak.forwardappmobile.features.reminders.data.models.Reminder

private inline fun bumpVersion(version: Long) = if (version == Long.MAX_VALUE) version else version + 1

// Generic bump/soft-delete helpers for entities with (updatedAt, syncedAt, isDeleted, version)

fun Context.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Context.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Goal.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Goal.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun BacklogItem.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun BacklogItem.softDelete(now: Long = System.currentTimeMillis()) =
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

fun InboxRecord.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun InboxRecord.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ActivityRecord.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ActivityRecord.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun LinkItemEntity.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun LinkItemEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ContextLog.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ContextLog.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ScriptEntity.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ScriptEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun AttachmentEntity.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun AttachmentEntity.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ContextAttachmentCrossRef.bumpSync(now: Long = System.currentTimeMillis()) =
    copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun ContextAttachmentCrossRef.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayPlan.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayPlan.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayTask.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DayTask.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DailyMetric.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun DailyMetric.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Reminder.bumpSync(now: Long = System.currentTimeMillis()) = copy(updatedAt = now, syncedAt = null, version = bumpVersion(version))

fun Reminder.softDelete(now: Long = System.currentTimeMillis()) =
    copy(isDeleted = true, updatedAt = now, syncedAt = null, version = bumpVersion(version))
