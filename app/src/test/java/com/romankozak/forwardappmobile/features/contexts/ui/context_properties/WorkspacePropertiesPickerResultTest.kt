package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityRefCodec
import org.junit.Test

class WorkspacePropertiesPickerResultTest {
    @Test
    fun `picker result is consumed exactly once`() {
        val handle = SavedStateHandle(
            mapOf(
                PENDING_WORKSPACE_BINDING_PICKER_KEY to true,
                CANONICAL_ENTITY_PICKER_RESULT_KEY to
                    listOf(CanonicalEntityRefCodec.encode(CanonicalEntityRef.Orientation("orientation-1"))),
            ),
        )
        val received = mutableListOf<Set<CanonicalEntityRef>>()

        assertThat(consumeWorkspaceBindingPickerResult(handle, received::add)).isTrue()
        assertThat(consumeWorkspaceBindingPickerResult(handle, received::add)).isFalse()
        assertThat(received).containsExactly(setOf(CanonicalEntityRef.Orientation("orientation-1")))
    }
}
