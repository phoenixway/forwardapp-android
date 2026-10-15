package com.romankozak.forwardappmobile.features.entitypicker

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalEntityPickerContractTest {
    private val orientation = CanonicalEntityRef.Orientation("same")
    private val aspect = CanonicalEntityRef.Aspect("same")
    private val workspace = CanonicalEntityRef.Workspace("same")

    @Test
    fun `typed refs round trip without collapsing equal string ids`() {
        val refs = listOf(orientation, aspect, workspace)
        assertThat(refs.map { CanonicalEntityRefCodec.decode(CanonicalEntityRefCodec.encode(it)) })
            .containsExactlyElementsIn(refs).inOrder()
        assertThat(refs.toSet()).hasSize(3)
    }

    @Test
    fun `single selection replaces current identity`() {
        val request = CanonicalEntityPickerRequest(setOf(CanonicalEntityClass.ORIENTATION))
        assertThat(togglePickerSelection(request, setOf(CanonicalEntityRef.Orientation("old")), orientation))
            .containsExactly(orientation)
    }

    @Test
    fun `multi selection toggles and preserves preselection`() {
        val request = CanonicalEntityPickerRequest(
            allowedClasses = setOf(CanonicalEntityClass.ASPECT),
            selectionMode = CanonicalEntitySelectionMode.MULTI,
            initiallySelected = setOf(aspect),
        )
        assertThat(togglePickerSelection(request, request.initiallySelected, CanonicalEntityRef.Aspect("other")))
            .containsExactly(aspect, CanonicalEntityRef.Aspect("other"))
        assertThat(togglePickerSelection(request, request.initiallySelected, aspect)).isEmpty()
    }

    @Test
    fun `wrong class and explicit exclusion fail closed`() {
        val request = CanonicalEntityPickerRequest(
            allowedClasses = setOf(CanonicalEntityClass.WORKSPACE),
            excludedRefs = setOf(workspace),
        )
        assertThrows(IllegalArgumentException::class.java) { togglePickerSelection(request, emptySet(), aspect) }
        assertThrows(IllegalArgumentException::class.java) { togglePickerSelection(request, emptySet(), workspace) }
    }

    @Test
    fun `typed navigation preserves picker constraints`() {
        val request = CanonicalEntityPickerRequest(
            allowedClasses = setOf(CanonicalEntityClass.ORIENTATION, CanonicalEntityClass.ASPECT),
            excludedRefs = setOf(aspect),
            selectionMode = CanonicalEntitySelectionMode.MULTI,
            initiallySelected = setOf(orientation),
        )
        val route = NavTargetRouter.routeOf(NavTarget.CanonicalEntityPicker(request))
        assertThat(route).contains("allowedClasses=")
        assertThat(route).contains("selectionMode=MULTI")
        assertThat(route).contains("excludedRefs=")
        assertThat(route).contains("initiallySelected=")
    }
}
