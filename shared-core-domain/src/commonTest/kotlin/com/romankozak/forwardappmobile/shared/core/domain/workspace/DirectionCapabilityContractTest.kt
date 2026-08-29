package com.romankozak.forwardappmobile.shared.core.domain.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DirectionCapabilityContractTest {
    @Test
    fun `configuration v1 round trips canonical payload`() {
        val configuration = DirectionCapabilityConfigurationV1(autoLinkChildWorkspaces = false)
        val encoded = DirectionCapabilityConfigurationCodec.encode(configuration)

        assertEquals("{\"autoLinkChildWorkspaces\":false}", encoded)
        assertEquals(
            configuration,
            DirectionCapabilityConfigurationCodec.decode(
                DirectionCapabilityConfigurationCodec.CURRENT_VERSION,
                encoded,
            ),
        )
    }

    @Test
    fun `configuration rejects unknown version and extra fields`() {
        assertFailsWith<IllegalArgumentException> {
            DirectionCapabilityConfigurationCodec.decode(
                version = 2,
                raw = "{\"autoLinkChildWorkspaces\":true}",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DirectionCapabilityConfigurationCodec.decode(
                version = 1,
                raw = "{\"autoLinkChildWorkspaces\":true,\"unknown\":1}",
            )
        }
    }

    @Test
    fun `legacy row classification separates semantic content from Workspace link`() {
        assertEquals(
            LegacyDirectionRowKind.SEMANTIC_DIRECTION,
            classifyLegacyDirectionRow(null),
        )
        assertEquals(
            LegacyDirectionRowKind.SEMANTIC_DIRECTION,
            classifyLegacyDirectionRow("  "),
        )
        assertEquals(
            LegacyDirectionRowKind.LINKED_ENTRY_REQUIRES_REVIEW,
            classifyLegacyDirectionRow("context-id"),
        )
    }
}
