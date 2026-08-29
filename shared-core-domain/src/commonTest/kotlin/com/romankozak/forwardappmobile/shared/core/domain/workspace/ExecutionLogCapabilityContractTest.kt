package com.romankozak.forwardappmobile.shared.core.domain.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExecutionLogCapabilityContractTest {
    @Test
    fun `v1 codec round trips canonical empty configuration`() {
        val encoded = ExecutionLogCapabilityConfigurationCodec.encode()

        assertEquals("{}", encoded)
        assertEquals(
            ExecutionLogCapabilityConfigurationV1,
            ExecutionLogCapabilityConfigurationCodec.decode(
                ExecutionLogCapabilityConfigurationCodec.CURRENT_VERSION,
                encoded,
            ),
        )
    }

    @Test
    fun `unknown version is rejected for mutation`() {
        assertFailsWith<IllegalArgumentException> {
            ExecutionLogCapabilityConfigurationCodec.decode(
                version = 2,
                raw = """{"future":true}""",
            )
        }
    }

    @Test
    fun `v1 rejects non empty payload`() {
        assertFailsWith<IllegalArgumentException> {
            ExecutionLogCapabilityConfigurationCodec.decode(
                version = 1,
                raw = """{"unexpected":true}""",
            )
        }
    }
}
