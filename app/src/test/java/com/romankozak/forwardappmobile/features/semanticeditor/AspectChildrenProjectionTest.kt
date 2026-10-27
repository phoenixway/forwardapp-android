package com.romankozak.forwardappmobile.features.semanticeditor

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspect
import org.junit.Test

class AspectChildrenProjectionTest {
    @Test fun `only direct canonical children appear not grandparents grandchildren roots or unrelated aspects`() {
        val rows = listOf(aspect("parent"), aspect("current", "parent"), aspect("child", "current"),
            aspect("grandchild", "child"), aspect("other"))
        assertThat(aspectDirectChildren("current", rows)).containsExactly(AspectChildItem("child", "Title child"))
        assertThat(aspectDirectChildren("missing", rows)).isEmpty()
    }

    @Test fun `canonical sibling order wins over title with stable exact-id tie break`() {
        val rows = listOf(aspect("z", "p", 0), aspect("b", "p", 2), aspect("a", "p", 2))
        assertThat(aspectDirectChildren("p", rows).map { it.aspectId }).containsExactly("z", "a", "b").inOrder()
        assertThat(aspectDirectChildren("p", rows.reversed())).isEqualTo(aspectDirectChildren("p", rows))
    }

    @Test fun `deleted and malformed nonAspect endpoints do not become live children`() {
        val deleted = aspect("deleted", "p").let { it.copy(subject = it.subject.copy(isDeleted = true)) }
        val wrongType = aspect("orientation", "p").let { it.copy(subject = it.subject.copy(subjectType = "ORIENTATION")) }
        assertThat(aspectDirectChildren("p", listOf(deleted, wrongType, aspect("live", "p"))).map { it.aspectId })
            .containsExactly("live")
    }

    @Test fun `same-title children navigate to their own exact semantic identity`() {
        val a = aspect("a", "p").let { it.copy(subject = it.subject.copy(title = "Same")) }
        val b = aspect("b", "p").let { it.copy(subject = it.subject.copy(title = "Same")) }
        val rows = aspectDirectChildren("p", listOf(b, a))
        assertThat(rows.map { it.destination() }).containsExactly(NavTarget.SemanticEntityEditor("a"),
            NavTarget.SemanticEntityEditor("b")).inOrder()
    }

    private fun aspect(id: String, parent: String? = null, order: Long = 0) = CanonicalAspect(
        ManagedSubjectEntity(id, "ASPECT", "Title $id", null, 1, 1, null, false, 1),
        AspectEntity(id, parent, order, false))
}
