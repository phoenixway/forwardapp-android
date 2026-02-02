package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DailyMetric
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask

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
