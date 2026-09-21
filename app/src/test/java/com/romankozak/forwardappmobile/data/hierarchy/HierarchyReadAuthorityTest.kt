package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.currentHierarchyPlacementAuthorityMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyReadAuthorityTest {
    @Test
    fun `production default remains CURRENT_PRE_CUTOVER`() {
        assertEquals(
            HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER,
            currentHierarchyPlacementAuthorityMode(),
        )
    }

    @Test
    fun `CURRENT mode invokes only current reader`() {
        var currentCalled = false
        var v2Called = false

        val result =
            HierarchyReadAuthorityRouter(
                HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER,
            ).route(
                currentPreCutover = {
                    currentCalled = true
                    "current"
                },
                v2Authority = {
                    v2Called = true
                    "v2"
                },
            )

        assertEquals("current", result)
        assertTrue(currentCalled)
        assertFalse(v2Called)
    }

    @Test
    fun `V2 mode invokes only V2 reader`() {
        var currentCalled = false
        var v2Called = false

        val result =
            HierarchyReadAuthorityRouter(
                HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            ).route(
                currentPreCutover = {
                    currentCalled = true
                    "current"
                },
                v2Authority = {
                    v2Called = true
                    "v2"
                },
            )

        assertEquals("v2", result)
        assertFalse(currentCalled)
        assertTrue(v2Called)
    }

    @Test
    fun `V2 failure escapes without CURRENT fallback`() {
        var currentCalled = false

        val failure =
            runCatching {
                HierarchyReadAuthorityRouter(
                    HierarchyPlacementAuthorityMode.V2_AUTHORITY,
                ).route(
                    currentPreCutover = {
                        currentCalled = true
                        "fallback"
                    },
                    v2Authority = {
                        error("malformed V2")
                    },
                )
            }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertFalse(currentCalled)
    }
}
