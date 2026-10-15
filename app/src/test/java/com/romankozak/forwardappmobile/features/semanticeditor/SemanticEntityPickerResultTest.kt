package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityRefCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticEntityPickerResultTest {
    @Test
    fun `picker result is typed and consumed exactly once`() {
        val handle =
            SavedStateHandle(
                mapOf(
                    CANONICAL_ENTITY_PICKER_RESULT_KEY to
                        listOf(CanonicalEntityRefCodec.encode(CanonicalEntityRef.Aspect("aspect-1"))),
                    PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY to SemanticRelationPickerKind.ASPECT.name,
                ),
            )
        val received = mutableListOf<Pair<SemanticRelationPickerKind, Set<CanonicalEntityRef>>>()

        assertTrue(consumeSemanticRelationPickerResult(handle) { kind, refs -> received += kind to refs })
        assertFalse(consumeSemanticRelationPickerResult(handle) { kind, refs -> received += kind to refs })

        assertEquals(
            listOf(
                SemanticRelationPickerKind.ASPECT to setOf(CanonicalEntityRef.Aspect("aspect-1")),
            ),
            received,
        )
    }
}
