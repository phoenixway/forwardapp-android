package com.romankozak.forwardappmobile.core.navigation

import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticEntityEditorNavigationContractTest {
    @Test
    fun `semantic editor route uses canonical subject identity only`() {
        val route = NavTargetRouter.routeOf(NavTarget.SemanticEntityEditor("subject-1"))

        assertTrue(route.startsWith("semantic_entity_editor_screen/subject-1"))
        assertFalse(route.contains("context"))
        assertFalse(route.contains("workspace"))
        assertTrue(NavigationRoutes.SEMANTIC_ENTITY_EDITOR_PATTERN.contains("{subjectId}"))
    }

    @Test
    fun `semantic subject id is safely encoded`() {
        val route = NavTargetRouter.routeOf(NavTarget.SemanticEntityEditor("subject/with spaces"))

        assertTrue(route.contains("%2F"))
        assertFalse(route.contains("subject/with spaces"))
    }
}
