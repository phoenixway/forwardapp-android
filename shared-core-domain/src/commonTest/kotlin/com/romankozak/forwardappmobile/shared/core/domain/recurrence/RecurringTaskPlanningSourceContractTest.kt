package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.recurrence.*
import kotlin.test.*

class RecurringTaskPlanningSourceContractTest {
    private fun template(kind: String? = null, id: String? = null) = createRecurringTaskTemplateForJs(
        "Task", null, "legacy-goal", emptyArray(), emptyArray(), "MEDIUM", null, 0, null, null, null, kind, id,
    )

    @Test fun legacyTemplateDoesNotInferSourceFromGoal() {
        assertNull(template().planningSource)
    }

    @Test fun typedSourcesPreserveExactIdentityAcrossJsBoundary() {
        for (kind in RecurringTaskSourceKind.entries) {
            val source = template(kind.name, "same-raw-id").planningSource!!
            assertEquals(kind, source.kind)
            assertEquals("same-raw-id", source.id)
        }
        assertNotEquals(template("ORIENTATION", "same-raw-id").planningSource, template("WORKSPACE", "same-raw-id").planningSource)
    }

    @Test fun malformedAndUnsupportedSourcesFailClosed() {
        assertFails { template("CONTEXT", "id") }
        assertFails { template("ORIENTATION", null) }
        assertFails { template(null, "id") }
        assertFails { template("ASPECT", "  ") }
    }
}
