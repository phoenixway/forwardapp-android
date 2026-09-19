package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HierarchyProjectNavigationTest {
    @Test
    fun shellFreeExactSystemUsesHierarchyReadRouteWithCanonicalTitle() {
        val presentation = presentation(SystemContexts.INBOX.raw, "Canonical Inbox")

        assertEquals(
            HierarchyProjectNavigation.HierarchyRead(SystemContexts.INBOX.raw, "Canonical Inbox"),
            resolveHierarchyProjectNavigation(
                projectId = SystemContexts.INBOX.raw,
                presentation = presentation,
            ),
        )
    }

    @Test
    fun ordinaryProjectLikeUsesDetailRouteWithPresentationTitle() {
        assertEquals(
            HierarchyProjectNavigation.ContextDetail("ordinary", "Projected"),
            resolveHierarchyProjectNavigation(
                projectId = "ordinary",
                presentation = presentation("ordinary", "Projected"),
            ),
        )
        assertEquals(
            HierarchyProjectNavigation.HierarchyRead(SystemContexts.INBOX.raw, "Canonical Inbox"),
            resolveHierarchyProjectNavigation(
                projectId = SystemContexts.INBOX.raw,
                presentation = presentation(SystemContexts.INBOX.raw, "Canonical Inbox"),
            ),
        )
    }

    @Test
    fun missingPresentationFailsClosed() {
        assertNull(
            resolveHierarchyProjectNavigation(
                projectId = "ordinary",
                presentation = null,
            ),
        )
    }

    @Test
    fun missingShellFreeIdentityFailsClosedButAdmittedNonSystemOwnerOpensDetail() {
        assertNull(
            resolveHierarchyProjectNavigation(
                projectId = SystemContexts.INBOX.raw,
                presentation = null,
            ),
        )
        assertEquals(
            HierarchyProjectNavigation.ContextDetail("retired-owner", "Restored"),
            resolveHierarchyProjectNavigation(
                projectId = "retired-owner",
                presentation = presentation("retired-owner", "Restored"),
            ),
        )
    }

    @Test
    fun historicalNonReservedSysIdRemainsAnOrdinaryProjectLike() {
        assertEquals(
            HierarchyProjectNavigation.ContextDetail("sys_custom", "Historical"),
            resolveHierarchyProjectNavigation(
                projectId = "sys_custom",
                presentation = presentation("sys_custom", "Historical"),
            ),
        )
    }

    private fun presentation(id: String, name: String) =
        HierarchyContextPresentationNode(
            id = id,
            name = name,
            description = null,
            parentId = null,
            order = 0L,
            roleCode = null,
            tags = emptyList(),
        )

}
