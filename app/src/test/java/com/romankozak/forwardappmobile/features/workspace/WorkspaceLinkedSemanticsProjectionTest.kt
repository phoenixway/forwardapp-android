package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class WorkspaceLinkedSemanticsProjectionTest {

    @Test
    fun `Orientation and Aspect preserve canonical semantic identity`() {
        val links = projectWorkspaceSemanticLinks(
            listOf(
                binding("orientation-1", WorkspaceBindingType.SUPPORTS.name),
                binding("aspect-1", WorkspaceBindingType.REALIZES.name),
            ),
            listOf(
                orientation("orientation-1", "North star"),
                aspect("aspect-1", "Health"),
            ),
        )

        assertThat(links).hasSize(2)

        val orientation = links.single { it.ref is CanonicalEntityRef.Orientation }
        val aspect = links.single { it.ref is CanonicalEntityRef.Aspect }

        assertThat(orientation.title).isEqualTo("North star")
        assertThat(aspect.title).isEqualTo("Health")
        assertThat(orientation.navigationTarget())
            .isEqualTo(NavTarget.SemanticEntityEditor("orientation-1"))
        assertThat(aspect.navigationTarget())
            .isEqualTo(NavTarget.SemanticEntityEditor("aspect-1"))
    }

    @Test
    fun `multiple binding types collapse into one semantic entity row`() {
        val links = projectWorkspaceSemanticLinks(
            listOf(
                binding("orientation-1", WorkspaceBindingType.SUPPORTS.name),
                binding("orientation-1", WorkspaceBindingType.EMBODIES.name),
                binding("orientation-1", WorkspaceBindingType.SUPPORTS.name),
            ),
            listOf(orientation("orientation-1", "North star")),
        )

        assertThat(links).hasSize(1)
        assertThat(links.single().bindingTypes)
            .containsExactly("EMBODIES", "SUPPORTS").inOrder()
        assertThat(links.single().isNavigable).isTrue()
    }

    @Test
    fun `same raw ID across semantic classes never guesses a target`() {
        val links = projectWorkspaceSemanticLinks(
            listOf(binding("shared", WorkspaceBindingType.SUPPORTS.name)),
            listOf(
                orientation("shared", "Orientation"),
                aspect("shared", "Aspect"),
            ),
        )

        assertThat(links).hasSize(1)
        assertThat(links.single().isNavigable).isFalse()
        assertThat(links.single().navigationTarget()).isNull()
    }

    @Test
    fun `missing canonical target is visible but cannot navigate`() {
        val links = projectWorkspaceSemanticLinks(
            listOf(binding("missing", WorkspaceBindingType.MONITORS.name)),
            emptyList(),
        )

        assertThat(links).hasSize(1)
        assertThat(links.single().title).contains("недоступна")
        assertThat(links.single().bindingTypes).containsExactly("MONITORS")
        assertThat(links.single().navigationTarget()).isNull()
    }

    @Test
    fun `malformed binding type fails closed`() {
        val links = projectWorkspaceSemanticLinks(
            listOf(binding("orientation-1", "INVALID_BINDING_TYPE")),
            listOf(orientation("orientation-1", "North star")),
        )

        assertThat(links.single().isNavigable).isFalse()
        assertThat(links.single().bindingTypes.single()).contains("Некоректний")
        assertThat(links.single().navigationTarget()).isNull()
    }

    @Test
    fun `deleted bindings are excluded`() {
        val links = projectWorkspaceSemanticLinks(
            listOf(
                binding(
                    "orientation-1",
                    WorkspaceBindingType.SUPPORTS.name,
                    deleted = true,
                ),
            ),
            listOf(orientation("orientation-1", "North star")),
        )

        assertThat(links).isEmpty()
    }

    @Test
    fun `Orientation and Aspect reuse existing canonical editor routes`() {
        val links = projectWorkspaceSemanticLinks(
            listOf(
                binding("orientation/with spaces", WorkspaceBindingType.SUPPORTS.name),
                binding("aspect-1", WorkspaceBindingType.REALIZES.name),
            ),
            listOf(
                orientation("orientation/with spaces", "North star"),
                aspect("aspect-1", "Aspect"),
            ),
        )

        val routes = links.mapNotNull { it.navigationTarget() }
            .map(NavTargetRouter::routeOf)

        assertThat(routes).containsExactly(
            NavigationRoutes.semanticEntityEditor("orientation/with spaces"),
            NavigationRoutes.semanticEntityEditor("aspect-1"),
        )
        assertThat(routes.single { it.contains("%2F") })
            .contains("orientation%2Fwith+spaces")
    }

    private fun orientation(id: String, title: String) =
        CanonicalEntitySummary(CanonicalEntityRef.Orientation(id), title)

    private fun aspect(id: String, title: String) =
        CanonicalEntitySummary(CanonicalEntityRef.Aspect(id), title)

    private fun binding(
        id: String,
        type: String,
        deleted: Boolean = false,
    ): WorkspaceBindingEntity =
        mockk<WorkspaceBindingEntity> {
            every { subjectId } returns id
            every { bindingType } returns type
            every { isDeleted } returns deleted
        }
}
