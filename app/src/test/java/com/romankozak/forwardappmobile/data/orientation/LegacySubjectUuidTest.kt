package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.UUID

class LegacySubjectUuidTest {
    @Test
    fun `uuid v5 matches RFC 4122 vector`() {
        val dnsNamespace = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

        assertEquals(
            UUID.fromString("21f7f8de-8051-5b89-8680-0195ef798b6a"),
            LegacySubjectUuid.uuidV5(dnsNamespace, "www.widgets.com"),
        )
    }

    @Test
    fun `legacy identity is stable and source type is part of identity`() {
        val goal = LegacySubjectRef(LegacyOrientationSourceType.GOAL, "same-id")
        val direction = LegacySubjectRef(LegacyOrientationSourceType.DIRECTION, "same-id")

        assertEquals(LegacySubjectUuid.resolve(goal), LegacySubjectUuid.resolve(goal))
        assertNotEquals(LegacySubjectUuid.resolve(goal), LegacySubjectUuid.resolve(direction))
        assertEquals(5, UUID.fromString(LegacySubjectUuid.resolve(goal)).version())
    }
}
