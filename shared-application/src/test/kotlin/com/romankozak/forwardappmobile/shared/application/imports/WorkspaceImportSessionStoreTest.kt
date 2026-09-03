package com.romankozak.forwardappmobile.shared.application.imports

import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSectionKind
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceImportSessionStoreTest {
    @Test
    fun `canonical Backlog item and section selection use placement ids`() {
        val store = WorkspaceImportSessionStore()

        store.dispatch(
            WorkspaceImportSessionIntent.ItemSelectionChanged(
                WorkspaceImportPreviewSectionKind.Backlog,
                "placement-a",
                true,
            ),
        )
        store.dispatch(
            WorkspaceImportSessionIntent.SectionSelectionChanged(
                WorkspaceImportPreviewSectionKind.Backlog,
                setOf("placement-b"),
                true,
            ),
        )

        assertEquals(
            setOf("placement-a", "placement-b"),
            store.state.value.selection.selectedWorkspaceBacklogEntryIds,
        )
    }
}
