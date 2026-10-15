package com.romankozak.forwardappmobile.core.navigation

import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** FK.10B.2a: Legacy Note routes are independent of NoteDocument and Context. */
class LegacyNoteNavigationContractTest {
    @Test
    fun `standalone Note navigates by Note ID without Context`() {
        val route = NavTargetRouter.routeOf(
            NavTarget.LegacyNote(noteId = "standalone-note"),
        )

        assertTrue(route.startsWith("legacy_note_screen/standalone-note"))
        assertFalse(route.contains("note_document_screen"))
        assertFalse(route.contains("workspaceId="))
    }

    @Test
    fun `optional Workspace locator does not replace Note identity`() {
        val route = NavTargetRouter.routeOf(
            NavTarget.LegacyNote(
                noteId = "shared-note",
                workspaceId = "workspace-a",
            ),
        )

        assertTrue(route.startsWith("legacy_note_screen/shared-note"))
        assertTrue(route.contains("workspaceId=workspace-a"))
        assertTrue(NavigationRoutes.LEGACY_NOTE_PATTERN.contains("{noteId}"))
        assertTrue(NavigationRoutes.LEGACY_NOTE_PATTERN.contains("{workspaceId}"))
    }

    @Test
    fun `Note ID is safely encoded in route`() {
        val route = NavTargetRouter.routeOf(
            NavTarget.LegacyNote(noteId = "note/with spaces"),
        )

        assertTrue(route.startsWith("legacy_note_screen/"))
        assertTrue(route.contains("%2F"))
        assertFalse(route.contains("note/with spaces"))
    }
}
