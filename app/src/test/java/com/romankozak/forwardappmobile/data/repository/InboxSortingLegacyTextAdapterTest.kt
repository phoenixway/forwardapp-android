package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingMode
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingRule
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class InboxSortingLegacyTextAdapterTest {
    @Test
    fun `compatibility decoder preserves legacy aliases defaults and first-match behavior`() {
        val configuration =
            InboxSortingLegacyTextAdapter.decode(
                """
                inbox:alpha
                inbox:oldest
                attachments:type
                backlog:unsupported
                ignored line
                """.trimIndent(),
            )

        assertEquals(
            InboxSortingCapabilityConfigurationV1(
                listOf(
                    WorkspaceSortingRule(WorkspaceSortingTarget.INBOX, WorkspaceSortingMode.ALPHA),
                    WorkspaceSortingRule(WorkspaceSortingTarget.CONNECTIONS, WorkspaceSortingMode.TYPE),
                ),
            ),
            configuration,
        )
    }

    @Test
    fun `typed policy projects back to existing text format`() {
        val configuration =
            InboxSortingCapabilityConfigurationV1(
                listOf(
                    WorkspaceSortingRule(WorkspaceSortingTarget.BACKLOG, WorkspaceSortingMode.OLDEST),
                    WorkspaceSortingRule(WorkspaceSortingTarget.CONNECTIONS, WorkspaceSortingMode.ALPHA),
                ),
            )

        assertEquals(
            "backlog:oldest\nconnections:alpha",
            InboxSortingLegacyTextAdapter.encode(configuration),
        )
    }
}
