package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import org.junit.Assert.*
import org.junit.Test

class DayFocusLinkNavigationTest {
    @Test fun `Workspace badge uses canonical availability-gated route not legacy Context screen`() {
        val link = RelatedLink(LinkType.CONTEXT, "exact-workspace", "current-treatment")
        assertEquals(NavigationRoutes.workspace("exact-workspace"), dayFocusWorkspaceRoute(link))
        assertNotEquals(NavigationRoutes.contextDetail("exact-workspace"), dayFocusWorkspaceRoute(link))
    }

    @Test fun `title changes never change exact destination and route arguments are encoded`() {
        val link = RelatedLink(LinkType.CONTEXT, "workspace/with spaces?", "Old title")
        assertEquals(NavigationRoutes.workspace(link.target), dayFocusWorkspaceRoute(link))
        assertEquals(dayFocusWorkspaceRoute(link), dayFocusWorkspaceRoute(link.copy(displayName = "New title")))
    }

    @Test fun `document checklist music and external links are never reinterpreted as Workspaces`() {
        LinkType.entries.filter { it != LinkType.CONTEXT }.forEach { type ->
            assertNull(dayFocusWorkspaceRoute(RelatedLink(type, "same-raw-id", "current-treatment")))
        }
    }
}
