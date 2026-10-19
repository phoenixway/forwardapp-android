package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticEntityLifecycleCoordinator
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BigPictureTargetActionsTest {
    @Test fun `typed semantic and Workspace identities resolve separately even with equal raw id`() {
        val targets = sourceDeletionTargets(listOf(subject("same", "ORIENTATION"), subject("aspect", "ASPECT")),
            listOf(OrientationEntity("same", "INTENT", null, "UNSET")), listOf(AspectEntity("aspect", null, 0, false)),
            emptyList(), listOf(workspace("same")))
        assertThat(targets.keys).containsExactly(CanonicalEntityRef.Orientation("same"), CanonicalEntityRef.Aspect("aspect"),
            CanonicalEntityRef.Workspace("same"))
        targets.values.forEach { target ->
            assertThat(sourceDeleteMenuTarget(H1TypedNode.Occurrence("appearance"), target)).isEqualTo(target)
            assertThat(sourceDeleteMenuTarget(H1TypedNode.Container("section"), target)).isNull()
        }
    }

    @Test fun `unsupported specialized and unknown kinds are not advertised`() {
        val unsupported = OrientationKind.entries.filterNot { it in com.romankozak.forwardappmobile.data.orientation.genericDeletableOrientationKinds }
        (unsupported.map { it.name } + "UNKNOWN").forEach { kind ->
            assertThat(sourceDeletionTargets(listOf(subject("o", "ORIENTATION")), listOf(OrientationEntity("o", kind, null, "UNSET")),
                emptyList(), emptyList(), emptyList())).isEmpty()
        }
    }

    @Test fun `legacy mappings missing nodes deleted identities and reserved Workspace sources fail closed`() {
        val mapping = LegacySubjectMappingEntity("mapping", "CONTEXT", "legacy", "mapped", 1, "CUT_OVER", 1, 1, null, true, 1)
        assertThat(sourceDeletionTargets(listOf(subject("mapped", "ASPECT"), subject("missing", "ASPECT"), subject("deleted", "ORIENTATION").copy(isDeleted = true)),
            listOf(OrientationEntity("deleted", "INTENT", null, "UNSET")), listOf(AspectEntity("mapped", null, 0, false)), listOf(mapping),
            listOf(workspace("legacy").copy(provenance = "CONTEXT_BACKED", sourceContextId = "legacy"),
                workspace(com.romankozak.forwardappmobile.core.context.SystemContexts.INBOX.raw).copy(provenance = "CANONICAL_ONLY"),
                workspace("dead").copy(isDeleted = true)))).isEmpty()
    }

    @Test fun `source confirmation explicitly names class title and every appearance`() {
        listOf(CanonicalEntityRef.Orientation("o"), CanonicalEntityRef.Aspect("a"), CanonicalEntityRef.Workspace("w")).forEach { ref ->
            val target = BigPictureSourceDeleteTarget(ref, "Exact title", 1)
            assertThat(target.confirmationTitle).contains("Exact title")
            assertThat(target.confirmationTitle).contains(target.classLabel)
            assertThat(target.confirmationText).contains("Усі")
            assertThat(target.confirmationText).contains("появи в Big Picture")
            assertThat(target.actionLabel).startsWith("Видалити")
            assertThat(target.actionLabel).isNotEqualTo("Прибрати з Big Picture")
        }
    }

    @Test fun `dispatch uses domain authority and exact canonical id never occurrence id`() = runTest {
        val semantic = mockk<CanonicalSemanticEntityLifecycleCoordinator>(relaxed = true)
        val workspace = mockk<CanonicalWorkspaceRepository>(relaxed = true)
        val actions = BigPictureTargetActions(mockk(), semantic, workspace)
        actions.deleteSource(BigPictureSourceDeleteTarget(CanonicalEntityRef.Orientation("o"), "Orientation", 7))
        actions.deleteSource(BigPictureSourceDeleteTarget(CanonicalEntityRef.Aspect("a"), "Aspect", 8))
        actions.deleteSource(BigPictureSourceDeleteTarget(CanonicalEntityRef.Workspace("w"), "Workspace", 9))
        coVerify(exactly = 1) { semantic.tombstone("o", ManagedSubjectType.ORIENTATION, 7, any()) }
        coVerify(exactly = 1) { semantic.tombstone("a", ManagedSubjectType.ASPECT, 8, any()) }
        coVerify(exactly = 1) { workspace.tombstone("w", any(), 9) }
    }

    private fun subject(id: String, type: String) = ManagedSubjectEntity(id, type, "Title", null, 1, 1, null, false, 1)
    private fun workspace(id: String) = WorkspaceEntity(id, "Workspace", null, null, 1, 1, null, false, 1, "STANDALONE", null)
}
