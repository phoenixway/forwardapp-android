package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyMixedDomainCommandSemanticsTest {
    @Test
    fun `Context PRIMARY CUT moves PRIMARY and keeps Direction separate`() {
        val selected = context("primary", "child", PlacementKind.PRIMARY, parent = "old")
        val destination = workspaceTarget("new-parent")

        val plan =
            HierarchyMixedDomainCommandSemantics.planContextCutToParent(
                selected = selected,
                primaryOccurrence = selected.occurrence,
                destinationParentPlacementId = PlacementId("new-parent-occ"),
                destinationParentTarget = destination,
            )

        assertEquals(
            listOf(
                HierarchyStructuralOperand.MoveOccurrence(
                    PlacementId("primary"),
                    PlacementId("new-parent-occ"),
                ),
            ),
            plan.structural,
        )
        assertEquals(
            listOf(
                HierarchySemanticOperand.EnsureDirectionFrontLinkIfEnabled(
                    parentWorkspaceTarget = destination,
                    childWorkspaceTarget = workspaceTarget("child"),
                ),
            ),
            plan.semantic,
        )
    }

    @Test
    fun `Context LINK CUT removes selected LINK then moves target PRIMARY`() {
        val selected = context("selected-link", "child", PlacementKind.LINK, parent = "link-parent")
        val primary =
            occurrence(
                id = "primary",
                target = workspaceTarget("child"),
                kind = PlacementKind.PRIMARY,
                parent = "primary-parent",
            )

        val plan =
            HierarchyMixedDomainCommandSemantics.planContextCutToParent(
                selected = selected,
                primaryOccurrence = primary,
                destinationParentPlacementId = PlacementId("destination"),
                destinationParentTarget = workspaceTarget("destination-target"),
            )

        assertEquals(
            listOf(
                HierarchyStructuralOperand.RemoveOccurrence(PlacementId("selected-link")),
                HierarchyStructuralOperand.MoveOccurrence(
                    PlacementId("primary"),
                    PlacementId("destination"),
                ),
            ),
            plan.structural,
        )
    }

    @Test
    fun `Context LINK CUT into same displayed parent does not remove selected LINK`() {
        val selected = context("link", "child", PlacementKind.LINK, parent = "destination")
        val primary =
            occurrence(
                id = "primary",
                target = workspaceTarget("child"),
                kind = PlacementKind.PRIMARY,
                parent = "old-primary-parent",
            )

        val plan =
            HierarchyMixedDomainCommandSemantics.planContextCutToParent(
                selected = selected,
                primaryOccurrence = primary,
                destinationParentPlacementId = PlacementId("destination"),
                destinationParentTarget = workspaceTarget("destination-target"),
            )

        assertEquals(
            listOf(
                HierarchyStructuralOperand.MoveOccurrence(
                    PlacementId("primary"),
                    PlacementId("destination"),
                ),
            ),
            plan.structural,
        )
    }

    @Test
    fun `Workspace COPY into concrete parent is shallow clone plus PRIMARY`() {
        val source = workspace("source-occ", "source")
        val intent =
            intent(
                HierarchyClipboardOperation.COPY,
                source,
                HierarchyClipboardDestination.ParentOccurrence(PlacementId("parent")),
            )

        val decision = HierarchyMixedDomainCommandSemantics.planWorkspacePasteIntoParent(intent)
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyTargetOperand.CloneWorkspaceShallow(
                    sourceOccurrenceId = PlacementId("source-occ"),
                    sourceTarget = workspaceTarget("source"),
                ),
            ),
            plan.target,
        )
        assertEquals(
            listOf(
                HierarchyStructuralOperand.CreatePrimaryForClonedWorkspace(
                    sourceOccurrenceId = PlacementId("source-occ"),
                    parentPlacementId = PlacementId("parent"),
                ),
            ),
            plan.structural,
        )
    }

    @Test
    fun `Context LINK into concrete parent creates LINK only`() {
        val source = context("source", "child", PlacementKind.PRIMARY)
        val intent =
            intent(
                HierarchyClipboardOperation.LINK,
                source,
                HierarchyClipboardDestination.ParentOccurrence(PlacementId("parent")),
            )

        val decision =
            HierarchyMixedDomainCommandSemantics.planContextPasteIntoParent(
                intent = intent,
                primaryOccurrencesByTarget = mapOf(source.occurrence.target to source.occurrence),
                destinationParentTarget = workspaceTarget("parent-target"),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyStructuralOperand.CreateAppearance(
                    target = workspaceTarget("child"),
                    parentPlacementId = PlacementId("parent"),
                    placementKind = PlacementKind.LINK,
                ),
            ),
            plan.structural,
        )
        assertTrue(plan.target.isEmpty())
        assertTrue(plan.semantic.isEmpty())
    }

    @Test
    fun `Workspace CUT into Beacon is operational only`() {
        val source = workspace("workspace-occ", "workspace")
        val destination = beaconDestination()
        val decision =
            HierarchyMixedDomainCommandSemantics.planOwnerPasteIntoBeacon(
                intent(
                    HierarchyClipboardOperation.CUT,
                    source,
                    destination,
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertTrue(plan.structural.isEmpty())
        assertEquals(
            listOf(
                HierarchyOperationalOperand.ReplaceBeaconOwnerAssociation(
                    ownerTarget = workspaceTarget("workspace"),
                    legacyBeaconId = "target-beacon",
                ),
            ),
            plan.operational,
        )
    }

    @Test
    fun `Context COPY into Beacon is operational only`() {
        val source = context("context-occ", "context", PlacementKind.PRIMARY)
        val destination = beaconDestination()
        val decision =
            HierarchyMixedDomainCommandSemantics.planOwnerPasteIntoBeacon(
                intent(
                    HierarchyClipboardOperation.COPY,
                    source,
                    destination,
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertTrue(plan.structural.isEmpty())
        assertEquals(
            listOf(
                HierarchyOperationalOperand.AddBeaconOwnerAssociation(
                    ownerTarget = workspaceTarget("context"),
                    legacyBeaconId = "target-beacon",
                ),
            ),
            plan.operational,
        )
    }

    @Test
    fun `Context LINK CUT into NoBeacon removes LINK roots PRIMARY and clears owner`() {
        val source = context("link", "context", PlacementKind.LINK, parent = "linked-parent")
        val primary =
            occurrence(
                id = "primary",
                target = workspaceTarget("context"),
                kind = PlacementKind.PRIMARY,
                parent = "primary-parent",
            )

        val decision =
            HierarchyMixedDomainCommandSemantics.planPasteIntoNoBeacon(
                intent(
                    HierarchyClipboardOperation.CUT,
                    source,
                    HierarchyClipboardDestination.NoBeacon,
                ),
                primaryOccurrencesByTarget = mapOf(source.occurrence.target to primary),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyStructuralOperand.RemoveOccurrence(PlacementId("link")),
                HierarchyStructuralOperand.MoveOccurrence(PlacementId("primary"), null),
            ),
            plan.structural,
        )
        assertEquals(
            listOf(
                HierarchyOperationalOperand.ClearBeaconOwnerAssociations(
                    workspaceTarget("context"),
                ),
            ),
            plan.operational,
        )
    }

    @Test
    fun `COPY into NoBeacon is unsupported and never manufactures synthetic target`() {
        val source = context("primary", "context", PlacementKind.PRIMARY)

        val decision =
            HierarchyMixedDomainCommandSemantics.planPasteIntoNoBeacon(
                intent(
                    HierarchyClipboardOperation.COPY,
                    source,
                    HierarchyClipboardDestination.NoBeacon,
                ),
            )

        assertTrue(decision is HierarchyActionPlanDecision.Unsupported)
    }

    @Test
    fun `Beacon COPY and LINK into Beacon create LINK appearance and do not clone target`() {
        val source = beacon("source-link", PlacementKind.PRIMARY)

        listOf(
            HierarchyClipboardOperation.COPY,
            HierarchyClipboardOperation.LINK,
        ).forEach { operation ->
            val decision =
                HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoBeacon(
                    intent(
                        operation,
                        source,
                        beaconDestination(),
                    ),
                )
            val plan = (decision as HierarchyActionPlanDecision.Planned).plan

            assertEquals(
                listOf(
                    HierarchyStructuralOperand.CreateAppearance(
                        target = managedSubjectTarget("beacon-subject"),
                        parentPlacementId = PlacementId("target-beacon-occ"),
                        placementKind = PlacementKind.LINK,
                    ),
                ),
                plan.structural,
            )
            assertTrue(plan.semantic.isEmpty())
            assertTrue(plan.operational.isEmpty())
            assertTrue(plan.target.isEmpty())
        }
    }

    @Test
    fun `true Beacon duplicate is target clone and not clipboard appearance`() {
        val source = beacon("source", PlacementKind.PRIMARY)

        val plan =
            HierarchyMixedDomainCommandSemantics.planTrueBeaconDuplicate(source)

        assertTrue(plan.structural.isEmpty())
        assertEquals(
            listOf(
                HierarchyTargetOperand.CloneBeaconTarget(
                    sourceTarget = managedSubjectTarget("beacon-subject"),
                    legacyBeaconId = "legacy-beacon",
                ),
            ),
            plan.target,
        )
    }

    @Test
    fun `Beacon PRIMARY CUT into Group moves exact root assigns scope and reconciles PART_OF`() {
        val source = beacon("beacon-primary", PlacementKind.PRIMARY)

        val decision =
            HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoGroup(
                intent(
                    HierarchyClipboardOperation.CUT,
                    source,
                    HierarchyClipboardDestination.Group("group-subject"),
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyStructuralOperand.MoveOccurrence(
                    PlacementId("beacon-primary"),
                    null,
                ),
            ),
            plan.structural,
        )
        assertEquals(
            listOf(
                HierarchyOccurrenceScopeOperand.SetRootGroupScope(
                    placementId = PlacementId("beacon-primary"),
                    groupSubjectId = "group-subject",
                ),
            ),
            plan.occurrenceScope,
        )
        assertEquals(
            listOf(
                HierarchySemanticOperand.ReconcileBeaconGroupMembershipFromRootScopes(
                    managedSubjectTarget("beacon-subject"),
                ),
            ),
            plan.semantic,
        )
    }

    @Test
    fun `Beacon CUT into NoGroup assigns explicit NoGroup scope and reconciles PART_OF`() {
        val source = beacon("beacon-primary", PlacementKind.PRIMARY)

        val decision =
            HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoGroup(
                intent(
                    HierarchyClipboardOperation.CUT,
                    source,
                    HierarchyClipboardDestination.Group(null),
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyOccurrenceScopeOperand.SetRootGroupScope(
                    placementId = PlacementId("beacon-primary"),
                    groupSubjectId = null,
                ),
            ),
            plan.occurrenceScope,
        )
        assertEquals(
            listOf(
                HierarchySemanticOperand.ReconcileBeaconGroupMembershipFromRootScopes(
                    managedSubjectTarget("beacon-subject"),
                ),
            ),
            plan.semantic,
        )
    }

    @Test
    fun `Beacon LINK into Group creates fused root LINK scope and reconciles PART_OF`() {
        val source = beacon("beacon-primary", PlacementKind.PRIMARY)

        val decision =
            HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoGroup(
                intent(
                    HierarchyClipboardOperation.LINK,
                    source,
                    HierarchyClipboardDestination.Group("group-subject"),
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertTrue(plan.structural.isEmpty())
        assertEquals(
            listOf(
                HierarchyOccurrenceScopeOperand.CreateRootLinkAppearance(
                    sourceOccurrenceId = PlacementId("beacon-primary"),
                    target = managedSubjectTarget("beacon-subject"),
                    groupSubjectId = "group-subject",
                ),
            ),
            plan.occurrenceScope,
        )
        assertEquals(
            listOf(
                HierarchySemanticOperand.ReconcileBeaconGroupMembershipFromRootScopes(
                    managedSubjectTarget("beacon-subject"),
                ),
            ),
            plan.semantic,
        )
    }

    @Test
    fun `Beacon PRIMARY CUT into Beacon moves exact selected PRIMARY occurrence`() {
        val source = beacon("beacon-primary", PlacementKind.PRIMARY)

        val decision =
            HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoBeacon(
                intent(
                    HierarchyClipboardOperation.CUT,
                    source,
                    beaconDestination(),
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyStructuralOperand.MoveOccurrence(
                    placementId = PlacementId("beacon-primary"),
                    newParentPlacementId = PlacementId("target-beacon-occ"),
                ),
            ),
            plan.structural,
        )
        assertEquals(
            listOf(
                HierarchyOccurrenceScopeOperand.RetireRootGroupScope(
                    PlacementId("beacon-primary"),
                ),
            ),
            plan.occurrenceScope,
        )
        assertEquals(
            listOf(
                HierarchySemanticOperand.ReconcileBeaconGroupMembershipFromRootScopes(
                    managedSubjectTarget("beacon-subject"),
                ),
            ),
            plan.semantic,
        )
        assertTrue(plan.operational.isEmpty())
        assertTrue(plan.target.isEmpty())
    }

    @Test
    fun `Beacon selected LINK CUT into Beacon moves exact LINK and leaves PRIMARY untouched`() {
        val selectedLink = beacon("beacon-link", PlacementKind.LINK)
        val primary = beacon("beacon-primary", PlacementKind.PRIMARY)

        assertEquals(primary.occurrence.target, selectedLink.occurrence.target)
        assertEquals(PlacementKind.LINK, selectedLink.occurrence.placementKind)

        val decision =
            HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoBeacon(
                intent(
                    HierarchyClipboardOperation.CUT,
                    selectedLink,
                    beaconDestination(),
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyStructuralOperand.MoveOccurrence(
                    placementId = PlacementId("beacon-link"),
                    newParentPlacementId = PlacementId("target-beacon-occ"),
                ),
            ),
            plan.structural,
        )
        assertTrue(
            plan.structural.none {
                it == HierarchyStructuralOperand.MoveOccurrence(
                    placementId = primary.occurrence.placementId,
                    newParentPlacementId = PlacementId("target-beacon-occ"),
                )
            },
        )
        assertEquals(
            listOf(
                HierarchyOccurrenceScopeOperand.RetireRootGroupScope(
                    PlacementId("beacon-link"),
                ),
            ),
            plan.occurrenceScope,
        )
        assertEquals(
            listOf(
                HierarchySemanticOperand.ReconcileBeaconGroupMembershipFromRootScopes(
                    managedSubjectTarget("beacon-subject"),
                ),
            ),
            plan.semantic,
        )
        assertTrue(plan.operational.isEmpty())
        assertTrue(plan.target.isEmpty())
    }

    @Test
    fun `Beacon selected LINK CUT into Group moves exact LINK and keeps PART_OF separate`() {
        val selectedLink = beacon("beacon-link", PlacementKind.LINK)

        val decision =
            HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoGroup(
                intent(
                    HierarchyClipboardOperation.CUT,
                    selectedLink,
                    HierarchyClipboardDestination.Group("group-subject"),
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyStructuralOperand.MoveOccurrence(
                    placementId = PlacementId("beacon-link"),
                    newParentPlacementId = null,
                ),
            ),
            plan.structural,
        )
        assertEquals(
            listOf(
                HierarchyOccurrenceScopeOperand.SetRootGroupScope(
                    placementId = PlacementId("beacon-link"),
                    groupSubjectId = "group-subject",
                ),
            ),
            plan.occurrenceScope,
        )
        assertEquals(
            listOf(
                HierarchySemanticOperand.ReconcileBeaconGroupMembershipFromRootScopes(
                    managedSubjectTarget("beacon-subject"),
                ),
            ),
            plan.semantic,
        )
        assertTrue(plan.operational.isEmpty())
        assertTrue(plan.target.isEmpty())
        assertEquals(PlacementKind.LINK, selectedLink.occurrence.placementKind)
    }

    @Test
    fun `Beacon CUT keeps duplicate same-target occurrences distinct`() {
        val primary = beacon("beacon-primary", PlacementKind.PRIMARY)
        val link = beacon("beacon-link", PlacementKind.LINK)
        val destination = beaconDestination()

        assertEquals(primary.occurrence.target, link.occurrence.target)

        val decision =
            HierarchyMixedDomainCommandSemantics.planBeaconPasteIntoBeacon(
                HierarchyClipboardIntent(
                    operation = HierarchyClipboardOperation.CUT,
                    sources = listOf(primary, link),
                    destination = destination,
                ),
            )
        val plan = (decision as HierarchyActionPlanDecision.Planned).plan

        assertEquals(
            listOf(
                HierarchyStructuralOperand.MoveOccurrence(
                    placementId = PlacementId("beacon-primary"),
                    newParentPlacementId = PlacementId("target-beacon-occ"),
                ),
                HierarchyStructuralOperand.MoveOccurrence(
                    placementId = PlacementId("beacon-link"),
                    newParentPlacementId = PlacementId("target-beacon-occ"),
                ),
            ),
            plan.structural,
        )
        assertEquals(2, plan.structural.size)
    }

    @Test
    fun `occurrence removal is not target deletion`() {
        val occurrencePlan =
            HierarchyMixedDomainCommandSemantics.planDestructiveIntent(
                HierarchyDestructiveIntent.RemoveOccurrence(PlacementId("occ")),
            )
        val targetPlan =
            HierarchyMixedDomainCommandSemantics.planDestructiveIntent(
                HierarchyDestructiveIntent.DeleteTarget(workspaceTarget("workspace")),
            )

        assertEquals(
            listOf(HierarchyStructuralOperand.RemoveOccurrence(PlacementId("occ"))),
            occurrencePlan.structural,
        )
        assertTrue(occurrencePlan.target.isEmpty())

        assertTrue(targetPlan.structural.isEmpty())
        assertEquals(
            listOf(HierarchyTargetOperand.DeleteTarget(workspaceTarget("workspace"))),
            targetPlan.target,
        )
    }

    @Test
    fun `standalone Workspace subtree delete stays target-domain operation`() {
        val plan =
            HierarchyMixedDomainCommandSemantics.planStandaloneWorkspaceSubtreeDelete(
                workspaceTarget("root"),
            )

        assertTrue(plan.structural.isEmpty())
        assertEquals(
            listOf(
                HierarchyTargetOperand.DeleteStandaloneWorkspaceTargetSubtree(
                    workspaceTarget("root"),
                ),
            ),
            plan.target,
        )
    }

    private fun intent(
        operation: HierarchyClipboardOperation,
        source: HierarchyClipboardOccurrence,
        destination: HierarchyClipboardDestination,
    ) = HierarchyClipboardIntent(
        operation = operation,
        sources = listOf(source),
        destination = destination,
    )

    private fun workspace(
        id: String,
        targetId: String,
    ) = HierarchyClipboardOccurrence(
        occurrence =
            occurrence(
                id = id,
                target = workspaceTarget(targetId),
                kind = PlacementKind.PRIMARY,
            ),
        sourceKind = HierarchyClipboardSourceKind.WORKSPACE,
    )

    private fun context(
        id: String,
        targetId: String,
        kind: PlacementKind,
        parent: String? = null,
    ) = HierarchyClipboardOccurrence(
        occurrence =
            occurrence(
                id = id,
                target = workspaceTarget(targetId),
                kind = kind,
                parent = parent,
            ),
        sourceKind = HierarchyClipboardSourceKind.CONTEXT_COMPATIBILITY,
    )

    private fun beacon(
        id: String,
        kind: PlacementKind,
    ) = HierarchyClipboardOccurrence(
        occurrence =
            occurrence(
                id = id,
                target = managedSubjectTarget("beacon-subject"),
                kind = kind,
            ),
        sourceKind = HierarchyClipboardSourceKind.BEACON,
        legacySourceId = "legacy-beacon",
    )

    private fun beaconDestination() =
        HierarchyClipboardDestination.BeaconOwner(
            legacyBeaconId = "target-beacon",
            occurrence =
                occurrence(
                    id = "target-beacon-occ",
                    target = managedSubjectTarget("target-beacon-subject"),
                    kind = PlacementKind.PRIMARY,
                ),
        )

    private fun occurrence(
        id: String,
        target: HierarchyTargetRef,
        kind: PlacementKind,
        parent: String? = null,
    ) = HierarchyOccurrenceRef(
        placementId = PlacementId(id),
        target = target,
        parentPlacementId = parent?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = 0L,
    )

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private fun managedSubjectTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)
}
