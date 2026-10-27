package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlin.test.*

class RelationsCapabilityContractTest {
    @Test fun registeredAsPresentationWithoutLegacyInference() {
        val definition = orientationCapabilityRegistry.single { it.type == WorkspaceCapabilityType.RELATIONS }
        assertEquals(WorkspaceCapabilityArchetype.PRESENTATION, definition.archetype)
        assertEquals(WorkspaceCapabilityAvailability.TARGET, definition.availability)
        assertEquals(1, definition.maxActiveInstances)
        assertTrue(definition.legacyIds.isEmpty())
        assertFalse(definition.autoMigrateWhenEnabled)
    }
    @Test fun configurationStrictlyVersionedAndOwnsNoData() {
        RelationsCapabilityConfigurationCodec.validate(1, RelationsCapabilityConfigurationCodec.encodeDefault())
        assertFailsWith<IllegalArgumentException> { RelationsCapabilityConfigurationCodec.validate(2, "{}") }
        assertFailsWith<IllegalArgumentException> { RelationsCapabilityConfigurationCodec.validate(1, "{\"relations\":[]}") }
    }
}
