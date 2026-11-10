package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceConnectionEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.features.attachments.data.ChecklistUsageRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChecklistUsagesTest {
    @Test fun canonicalWorkspaceLinksAreDeduplicatedAndDeletedTargetsAreExcluded() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            f.db.checklistDao().insertChecklist(ChecklistEntity(id = "list", name = "Checklist"))
            f.dao.upsertWorkspaceCapabilities(listOf(WorkspaceCapabilityInstanceEntity(
                "cap", "workspace", "CONNECTIONS", "default", 0, "ACTIVE", 1, "{}", 1, 1, null, false, 1)))
            f.db.attachmentDao().insertAttachments(listOf(
                AttachmentEntity(id = "a", attachmentType = "CHECKLIST", entityId = "list"),
                AttachmentEntity(id = "b", attachmentType = "CHECKLIST", entityId = "list"),
                AttachmentEntity(id = "other", attachmentType = "NOTE_DOCUMENT", entityId = "list")))
            f.db.workspaceConnectionDao().upsert(listOf(
                WorkspaceConnectionEntity("one", "workspace", "cap", "a", 0, 1, 1, null, false, 1),
                WorkspaceConnectionEntity("two", "workspace", "cap", "b", 1, 1, 1, null, false, 1),
                WorkspaceConnectionEntity("wrong-type", "workspace", "cap", "other", 2, 1, 1, null, false, 1)))
            val rows = f.db.attachmentDao().observeChecklistUsages("list").first()
            assertThat(rows).hasSize(1)
            assertThat(rows.single().destination()).isEqualTo(NavTarget.Workspace("workspace"))
            assertThat(f.db.attachmentDao().observeChecklistUsages("another-list").first()).isEmpty()
            f.db.openHelper.writableDatabase.execSQL("UPDATE workspaces SET isDeleted=1 WHERE id='workspace'")
            assertThat(f.db.attachmentDao().observeChecklistUsages("list").first()).isEmpty()
        }
    }

    @Test fun typedDestinationsNeverGuessOwnerIdentityFromTitle() {
        fun row(kind: String, id: String) = ChecklistUsageRow("MISSION", "old-id", "Duplicate title", kind, id)
        assertThat(row("WORKSPACE", "workspace-id").destination()).isEqualTo(NavTarget.Workspace("workspace-id"))
        assertThat(row("PARTICIPATION", "commitment-id").destination()).isEqualTo(NavTarget.PlanningParticipation("commitment-id"))
        assertThat(row("SEMANTIC", "subject-id").destination()).isEqualTo(NavTarget.SemanticEntityEditor("subject-id"))
        assertThat(row("TACTICS", "old-id").destination()).isEqualTo(NavTarget.TacticalManagement)
    }
}
