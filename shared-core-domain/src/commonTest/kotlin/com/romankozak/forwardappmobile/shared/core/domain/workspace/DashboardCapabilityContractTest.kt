package com.romankozak.forwardappmobile.shared.core.domain.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DashboardCapabilityContractTest {
    @Test
    fun `wire validator preserves dashboard configuration contract`() {
        assertEquals(
            emptyList(),
            validateDashboardCapabilityConfigurationWire(1, "{}").toList(),
        )
        assertEquals(
            listOf("Unsupported DASHBOARD configuration version: 2"),
            validateDashboardCapabilityConfigurationWire(2, "{}").toList(),
        )
        assertEquals(
            listOf("Invalid DASHBOARD configuration v1"),
            validateDashboardCapabilityConfigurationWire(1, "{\"unexpected\":true}").toList(),
        )
    }

    @Test
    fun `v1 codec round-trips canonical empty configuration`() {
        val encoded = DashboardCapabilityConfigurationCodec.encode()

        assertEquals("{}", encoded)
        assertEquals(
            DashboardCapabilityConfigurationV1,
            DashboardCapabilityConfigurationCodec.decode(
                DashboardCapabilityConfigurationCodec.CURRENT_VERSION,
                encoded,
            ),
        )
    }

    @Test
    fun `unknown version is rejected for mutation`() {
        assertFailsWith<IllegalArgumentException> {
            DashboardCapabilityConfigurationCodec.decode(
                version = 2,
                raw = """{"future":true}""",
            )
        }
    }

    @Test
    fun `v1 rejects non-empty payload`() {
        assertFailsWith<IllegalArgumentException> {
            DashboardCapabilityConfigurationCodec.decode(
                version = 1,
                raw = """{"unexpected":true}""",
            )
        }
    }
}
