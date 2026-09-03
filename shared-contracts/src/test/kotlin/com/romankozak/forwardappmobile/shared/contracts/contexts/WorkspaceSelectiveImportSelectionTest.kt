package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WorkspaceSelectiveImportSelectionTest {
    @Test
    fun `canonical BACKLOG placement ids round trip through shared selection contract`() {
        val selection =
            WorkspaceSelectiveImportSelection(
                selectedWorkspaceBacklogEntryIds = setOf("placement-a", "placement-b"),
            )

        val encoded = Json.encodeToString(selection)
        val decoded = Json.decodeFromString<WorkspaceSelectiveImportSelection>(encoded)

        assertEquals(selection, decoded)
        assertTrue(encoded.contains("selectedWorkspaceBacklogEntryIds"))
    }

    @Test
    fun `empty selection keeps canonical BACKLOG placement selection empty`() {
        assertTrue(WorkspaceSelectiveImportSelection().selectedWorkspaceBacklogEntryIds.isEmpty())
    }
}
