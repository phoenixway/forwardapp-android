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
                hasLegacyBacking = false,
            ),
        )
    }

    @Test
    fun rawBackedRowsUseContextDetailRouteWithPresentationTitle() {
        assertEquals(
            HierarchyProjectNavigation.ContextDetail("ordinary", "Projected"),
            resolveHierarchyProjectNavigation(
                projectId = "ordinary",
                presentation = presentation("ordinary", "Projected"),
                hasLegacyBacking = true,
            ),
        )
        assertEquals(
            HierarchyProjectNavigation.ContextDetail(SystemContexts.INBOX.raw, "Canonical Inbox"),
            resolveHierarchyProjectNavigation(
                projectId = SystemContexts.INBOX.raw,
                presentation = presentation(SystemContexts.INBOX.raw, "Canonical Inbox"),
                hasLegacyBacking = true,
            ),
        )
    }

    @Test
    fun rawBackingWithoutPresentationFailsClosed() {
        assertNull(
            resolveHierarchyProjectNavigation(
                projectId = "ordinary",
                presentation = null,
                hasLegacyBacking = true,
            ),
        )
    }

    @Test
    fun missingShellFreeIdentityFailsClosedButAdmittedNonSystemOwnerOpensDetail() {
        assertNull(
            resolveHierarchyProjectNavigation(
                projectId = SystemContexts.INBOX.raw,
                presentation = null,
                hasLegacyBacking = false,
            ),
        )
        assertEquals(
            HierarchyProjectNavigation.ContextDetail("retired-owner", "Restored"),
            resolveHierarchyProjectNavigation(
                projectId = "retired-owner",
                presentation = presentation("retired-owner", "Restored"),
                hasLegacyBacking = false,
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
