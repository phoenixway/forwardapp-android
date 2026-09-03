package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.data.models.sync.DiffResult
import com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus
import com.romankozak.forwardappmobile.core.data.models.sync.LegacyBackupDiff
import com.romankozak.forwardappmobile.core.data.models.sync.UpdatedItem

fun LegacyBackupDiff.toSelectable(): SelectableDatabaseContent {
    fun <T> mapDiff(
        diff: DiffResult<T>,
        updatedInfo: (UpdatedItem<T>) -> String? = { null },
    ): List<SelectableDiffItem<T>> {
        val newItems =
            diff.added.map {
                SelectableDiffItem(item = it, status = DiffStatus.NEW, isSelected = true, isSelectable = true)
            }
        val updatedItems =
            diff.updated.map {
                SelectableDiffItem(
                    item = it.incoming,
                    status = DiffStatus.UPDATED,
                    isSelected = true,
                    isSelectable = true,
                    changeInfo = updatedInfo(it),
                )
            }
        val deletedItems =
            diff.deleted.map {
                SelectableDiffItem(item = it, status = DiffStatus.DELETED, isSelected = false, isSelectable = false)
            }
        return newItems + updatedItems + deletedItems
    }

    return SelectableDatabaseContent(
        projects = mapDiff(this.projects),
        goals = mapDiff(this.goals),
        legacyNotes = mapDiff(this.legacyNotes),
        activityRecords = mapDiff(this.activityRecords),
        documents = mapDiff(this.documents),
        checklists = mapDiff(this.checklists),
        checklistItems = mapDiff(this.checklistItems),
        linkItems = mapDiff(this.linkItems),
        inboxRecords = mapDiff(this.inboxRecords),
        contextLogs = mapDiff(this.contextLogs),
        scripts = mapDiff(this.scripts),
        attachments = mapDiff(this.attachments),
        allContextAttachmentCrossRefs = this.contextAttachmentCrossRefs.added + this.contextAttachmentCrossRefs.updated.map { it.incoming },
    )
}
