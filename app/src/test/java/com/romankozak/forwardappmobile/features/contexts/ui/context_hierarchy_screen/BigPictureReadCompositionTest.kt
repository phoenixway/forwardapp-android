package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Assert.assertThrows
import org.junit.Test

class BigPictureReadCompositionTest {

    @Test
    fun `generic mixed root sections supersede historical root membership without rewriting H1`() {
        val placements = listOf(
            p("orientation", target = HierarchyTargetType.MANAGED_SUBJECT, order = 1),
            p("workspace", target = HierarchyTargetType.WORKSPACE, order = 2),
            p("free", target = HierarchyTargetType.WORKSPACE, order = 3),
        )
        val read = BigPictureReadComposition(
            nodes = projectBigPicture(placements, emptyList()),
            historicalScopes = listOf(
                scope("group:old", "orientation"),
                scope("NoBeacon", "workspace"),
            ),
            persisted = BigPicturePresentationSnapshot(
                containers = listOf(c("generic", order = 2)),
                memberships = listOf(m("orientation", "generic"), m("workspace", "generic")),
            ),
        )
        assertThat(read.historicalScopes.flatMap { it.rootPlacementIds }).isEmpty()
        assertThat(read.rootEntries().map { it.stableId }).containsExactly(
            "container:group:old",
            "container:NoBeacon",
            "container:presentation:generic",
            "occurrence:free",
        ).inOrder()
        assertThat(read.sectionEntries("presentation:generic").map { it.stableId })
            .containsExactly("occurrence:orientation", "occurrence:workspace").inOrder()
        assertThat(placements.map { it.parentPlacementId }).containsExactly(null, null, null)
    }

    @Test
    fun `nested sections at arbitrary H1 depth preserve occurrence identity and mixed order`() {
        val placements = listOf(
            p("root"),
            p("child", target = HierarchyTargetType.MANAGED_SUBJECT, parent = "root", order = 3),
            p("free-child", parent = "root", order = 2),
        )
        val read = BigPictureReadComposition(
            projectBigPicture(placements, emptyList()),
            emptyList(),
            BigPicturePresentationSnapshot(
                containers = listOf(
                    c("outer", anchor = "root", order = 1),
                    c("inner", anchor = "root", parent = "outer", order = 2),
                ),
                memberships = listOf(m("child", "inner")),
            ),
        )
        assertThat(read.occurrenceEntries("root").map { it.stableId })
            .containsExactly("container:presentation:outer", "occurrence:free-child").inOrder()
        assertThat(read.sectionEntries("presentation:outer").map { it.stableId })
            .containsExactly("container:presentation:inner")
        assertThat(read.sectionEntries("presentation:inner").map { it.stableId })
            .containsExactly("occurrence:child")
        assertThat(read.genericAncestors("presentation:inner").map { it.id })
            .containsExactly("presentation:outer", "presentation:inner").inOrder()
        assertThat(placements.single { it.id.value == "child" }.parentPlacementId)
            .isEqualTo(PlacementId("root"))
    }

    @Test
    fun `tombstoned section retains historical records but member returns to historical display`() {
        val placements = listOf(p("root"))
        val read = BigPictureReadComposition(
            projectBigPicture(placements, emptyList()),
            listOf(scope("group:old", "root")),
            BigPicturePresentationSnapshot(
                containers = listOf(c("old").copy(isDeleted = true)),
                memberships = listOf(m("root", "old")),
            ),
        )
        assertThat(read.rootScopes.map { it.id }).containsExactly("group:old")
        assertThat(read.historicalEntries("group:old").map { it.stableId })
            .containsExactly("occurrence:root")
        assertThat(read.rootEntries().map { it.stableId })
            .containsExactly("container:group:old")
    }

    @Test
    fun `invalid cross anchor live membership fails closed`() {
        val placements = listOf(p("a"), p("b"), p("child", parent = "a"))
        assertThrows(IllegalArgumentException::class.java) {
            BigPictureReadComposition(
                projectBigPicture(placements, emptyList()),
                emptyList(),
                BigPicturePresentationSnapshot(
                    containers = listOf(c("wrong", anchor = "b")),
                    memberships = listOf(m("child", "wrong")),
                ),
            )
        }
    }

    private fun scope(id: String, vararg ids: String) =
        BigPictureContainer(
            id, id, ids.mapTo(linkedSetOf(), ::PlacementId),
            BigPictureContainerOrigin.HISTORICAL_GROUP,
        )

    private fun c(
        id: String,
        anchor: String? = null,
        parent: String? = null,
        order: Long = 0,
    ) = BigPictureContainerSnapshot(
        id, "GENERAL", id, anchor, parent, order, 1, 1, null, false, 1,
    )

    private fun m(id: String, container: String) =
        BigPictureMembershipSnapshot(
            id, "GENERAL", container, 1, 1, null, false, 1,
        )

    private fun p(
        id: String,
        target: HierarchyTargetType = HierarchyTargetType.WORKSPACE,
        parent: String? = null,
        order: Long = 0,
    ) = HierarchyPlacement(
        PlacementId(id), HierarchyId.GENERAL,
        HierarchyTargetRef(target, id),
        parent?.let(::PlacementId), PlacementKind.PRIMARY,
        order, 1, 1, null, false, 1,
    )
}
