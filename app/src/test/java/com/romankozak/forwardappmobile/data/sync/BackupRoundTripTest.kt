package com.romankozak.forwardappmobile.data.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRoundTripTest {

    private val gson = Gson()

    @Test
    fun `exported backup restores list item order after import`() {
        val project = Project(
            id = "p1",
            name = "Backlog",
            description = null,
            parentId = null,
            systemKey = null,
            createdAt = 1L,
            updatedAt = 2L
        )
        val listItem = ListItem(
            id = "li1",
            projectId = project.id,
            itemType = "TASK",
            entityId = "task-123",
            order = 5L
        )

        val original = FullAppBackup(
            backupSchemaVersion = 2,
            database = DatabaseContent(
                projects = listOf(project),
                listItems = listOf(listItem)
            )
        )

        val json = gson.toJson(original)
        val parsed = gson.fromJson(json, FullAppBackup::class.java)

        assertEquals(listItem.order, parsed.database.listItems.single().order)
        assertEquals(original.database, parsed.database)

        val diff = buildDiff(
            local = original.database.listItems,
            incoming = parsed.database.listItems
        ) { it.id }

        assertTrue(diff.added.isEmpty())
        assertTrue(diff.updated.isEmpty())
        assertTrue(diff.deleted.isEmpty())
    }

    @Test
    fun `roundtrip keeps documents checklists attachments and orders`() {
        val project = Project(
            id = "pA",
            name = "ProjA",
            description = "desc",
            parentId = null,
            systemKey = null,
            createdAt = 10L,
            updatedAt = 20L
        )

        val doc = NoteDocumentEntity(
            id = "doc1",
            projectId = project.id,
            name = "Doc",
            createdAt = 30L,
            updatedAt = 40L,
            content = "content",
            lastCursorPosition = 5
        )
        val docItem = NoteDocumentItemEntity(
            id = "docItem1",
            listId = doc.id,
            parentId = null,
            content = "item",
            isCompleted = false,
            itemOrder = 2,
            createdAt = 50L,
            updatedAt = 60L
        )

        val checklist = ChecklistEntity(
            id = "cl1",
            projectId = project.id,
            name = "Checklist"
        )
        val checklistItem = ChecklistItemEntity(
            id = "cli1",
            checklistId = checklist.id,
            content = "check item",
            isChecked = true,
            itemOrder = 3
        )

        val attachment = AttachmentEntity(
            id = "att1",
            attachmentType = "NOTE_DOCUMENT",
            entityId = doc.id,
            ownerProjectId = project.id,
            createdAt = 70L,
            updatedAt = 80L
        )
        val crossRef = ProjectAttachmentCrossRef(
            projectId = project.id,
            attachmentId = attachment.id,
            attachmentOrder = -1
        )
        val script = ScriptEntity(
            id = "script1",
            projectId = project.id,
            name = "Script",
            description = "desc",
            content = "println(\"hi\")",
            createdAt = 81L,
            updatedAt = 82L
        )
        val recentProjectEntry = RecentProjectEntry(
            projectId = project.id,
            timestamp = 83L
        )

        val listItem = ListItem(
            id = "li2",
            projectId = project.id,
            itemType = "TASK",
            entityId = "task-xyz",
            order = 9L
        )

        val originalDb = DatabaseContent(
            projects = listOf(project),
            listItems = listOf(listItem),
            documents = listOf(doc),
            documentItems = listOf(docItem),
            checklists = listOf(checklist),
            checklistItems = listOf(checklistItem),
            attachments = listOf(attachment),
            projectAttachmentCrossRefs = listOf(crossRef),
            scripts = listOf(script),
            recentProjectEntries = listOf(recentProjectEntry)
        )
        val original = FullAppBackup(
            backupSchemaVersion = 2,
            database = originalDb
        )

        val json = gson.toJson(original)
        val parsed = gson.fromJson(json, FullAppBackup::class.java)

        assertEquals(original.database, parsed.database)

        val diffProjects = buildDiff(originalDb.projects, parsed.database.projects) { it.id }
        val diffDocs = buildDiff(originalDb.documents, parsed.database.documents) { it.id }
        val diffDocItems = buildDiff(originalDb.documentItems, parsed.database.documentItems) { it.id }
        val diffChecklists = buildDiff(originalDb.checklists, parsed.database.checklists) { it.id }
        val diffChecklistItems = buildDiff(originalDb.checklistItems, parsed.database.checklistItems) { it.id }
        val diffAttachments = buildDiff(originalDb.attachments, parsed.database.attachments) { it.id }
        val diffCrossRefs = buildDiff(originalDb.projectAttachmentCrossRefs, parsed.database.projectAttachmentCrossRefs) { "${it.projectId}-${it.attachmentId}" }
        val diffListItems = buildDiff(originalDb.listItems, parsed.database.listItems) { it.id }
        val diffScripts = buildDiff(originalDb.scripts, parsed.database.scripts) { it.id }
        val diffRecentEntries = buildDiff(originalDb.recentProjectEntries, parsed.database.recentProjectEntries) { it.projectId }

        assertTrue(diffProjects.added.isEmpty() && diffProjects.updated.isEmpty() && diffProjects.deleted.isEmpty())
        assertTrue(diffDocs.added.isEmpty() && diffDocs.updated.isEmpty() && diffDocs.deleted.isEmpty())
        assertTrue(diffDocItems.added.isEmpty() && diffDocItems.updated.isEmpty() && diffDocItems.deleted.isEmpty())
        assertTrue(diffChecklists.added.isEmpty() && diffChecklists.updated.isEmpty() && diffChecklists.deleted.isEmpty())
        assertTrue(diffChecklistItems.added.isEmpty() && diffChecklistItems.updated.isEmpty() && diffChecklistItems.deleted.isEmpty())
        assertTrue(diffAttachments.added.isEmpty() && diffAttachments.updated.isEmpty() && diffAttachments.deleted.isEmpty())
        assertTrue(diffCrossRefs.added.isEmpty() && diffCrossRefs.updated.isEmpty() && diffCrossRefs.deleted.isEmpty())
        assertTrue(diffListItems.added.isEmpty() && diffListItems.updated.isEmpty() && diffListItems.deleted.isEmpty())
        assertTrue(diffScripts.added.isEmpty() && diffScripts.updated.isEmpty() && diffScripts.deleted.isEmpty())
        assertTrue(diffRecentEntries.added.isEmpty() && diffRecentEntries.updated.isEmpty() && diffRecentEntries.deleted.isEmpty())
    }

    private fun <T> buildDiff(local: List<T>, incoming: List<T>, idSelector: (T) -> String): DiffResult<T> {
        val localMap = local.associateBy(idSelector)
        val incomingMap = incoming.associateBy(idSelector)

        val added = incomingMap.filterKeys { it !in localMap }.values.toList()
        val deleted = localMap.filterKeys { it !in incomingMap }.values.toList()

        val updated = incomingMap.mapNotNull { (id, incomingItem) ->
            val localItem = localMap[id]
            if (localItem != null && localItem != incomingItem) {
                UpdatedItem(local = localItem, incoming = incomingItem)
            } else {
                null
            }
        }

        return DiffResult(
            added = added,
            updated = updated,
            deleted = deleted,
        )
    }
}
