package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.tactical.tactical.TacticalMissionSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.attachments.LegacyNoteSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextInboxSortingSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingMode
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingTarget
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotRestoreCanonicalizerTest {
    private val subject = SnapshotRestoreCanonicalizerImpl()

    @Test
    fun `legacy MainBeacon owner requires original live Context evidence`() {
        val ref = MainBeaconContextCrossRefSnapshot(
            beaconId = "beacon",
            contextId = OWNER_ID,
            order = 0L,
        )

        val admitted =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID)),
                    mainBeaconContextCrossRefs = listOf(ref),
                ),
            )

        assertEquals(listOf(ref), admitted.mainBeaconContextCrossRefs)
        assertEquals(
            WorkspaceProvenance.CANONICAL_ONLY.name,
            requireNotNull(admitted.workspaces).single().provenance,
        )

        val missing =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        mainBeaconContextCrossRefs = listOf(ref),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(
            "Expected missing historical Context rejection: $missing",
            missing is IllegalArgumentException &&
                missing.message.orEmpty().contains("Historical MainBeacon owner"),
        )

        val deleted =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        contexts = listOf(context(OWNER_ID, isDeleted = true)),
                        mainBeaconContextCrossRefs = listOf(ref),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(
            "Expected deleted historical Context rejection: $deleted",
            deleted is IllegalArgumentException &&
                deleted.message.orEmpty().contains("Historical MainBeacon owner"),
        )
    }

    @Test
    fun `legacy MainBeacon association cannot claim unrelated same-id Workspace`() {
        val canonicalWorkspace =
            requireNotNull(
                subject.canonicalize(
                    SnapshotBundle(contexts = listOf(context(OWNER_ID))),
                ).workspaces,
            ).single()

        val ref = MainBeaconContextCrossRefSnapshot(
            beaconId = "beacon",
            contextId = OWNER_ID,
            order = 0L,
        )

        val missingHistoricalContext =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        workspaces = listOf(canonicalWorkspace),
                        mainBeaconContextCrossRefs = listOf(ref),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(
            "Expected same-id Workspace rejection: $missingHistoricalContext",
            missingHistoricalContext is IllegalArgumentException &&
                missingHistoricalContext.message.orEmpty()
                    .contains("Historical MainBeacon owner"),
        )

        val divergentWorkspace =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        contexts = listOf(context(OWNER_ID)),
                        workspaces = listOf(
                            canonicalWorkspace.copy(
                                provenance = WorkspaceProvenance.STANDALONE.name,
                            ),
                        ),
                        mainBeaconContextCrossRefs = listOf(ref),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(
            "Expected mismatched Workspace provenance rejection: $divergentWorkspace",
            divergentWorkspace is IllegalArgumentException &&
                divergentWorkspace.message.orEmpty()
                    .contains("Historical MainBeacon owner"),
        )
    }

    @Test
    fun `mixed historical MainBeacon owner rejects deleted source Context despite matching canonical Workspace`() {
        val canonicalWorkspace =
            requireNotNull(
                subject.canonicalize(
                    SnapshotBundle(contexts = listOf(context(OWNER_ID))),
                ).workspaces,
            ).single()

        assertEquals(
            WorkspaceProvenance.CANONICAL_ONLY.name,
            canonicalWorkspace.provenance,
        )

        val ref =
            MainBeaconContextCrossRefSnapshot(
                beaconId = "beacon",
                contextId = OWNER_ID,
                order = 0L,
            )

        val rejected =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        contexts = listOf(context(OWNER_ID, isDeleted = true)),
                        workspaces = listOf(canonicalWorkspace),
                        mainBeaconContextCrossRefs = listOf(ref),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(
            "Expected deleted historical owner rejection: $rejected",
            rejected is IllegalArgumentException &&
                rejected.message.orEmpty()
                    .contains("Historical MainBeacon owner"),
        )
    }

    @Test
    fun `canonical MainBeacon owner needs no historical Context row`() {
        val workspace =
            requireNotNull(
                subject.canonicalize(
                    SnapshotBundle(contexts = listOf(context(OWNER_ID))),
                ).workspaces,
            ).single()

        val ref = MainBeaconContextCrossRefSnapshot(
            beaconId = "beacon",
            contextId = OWNER_ID,
            order = 0L,
        )

        val result =
            subject.canonicalize(
                SnapshotBundle(
                    workspaces = listOf(workspace),
                    mainBeaconContextCrossRefs = listOf(ref),
                    hierarchyPlacements = emptyList(),
                    hierarchyPlacementGroupScopes = emptyList(),
                    hierarchyPlacementLinkedAppearances = emptyList(),
                ),
            )

        assertEquals(listOf(ref), result.mainBeaconContextCrossRefs)
    }


    @Test
    fun `pre canonical ordinary owner content becomes canonical restore payload`() {
        val bundle =
            SnapshotBundle(
                version = 2,
                exportedAt = 100L,
                contexts = listOf(context(OWNER_ID)),
                notes = listOf(note(NOTE_ID, OWNER_ID)),
                backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                inbox = listOf(inbox(OWNER_ID)),
            )

        val result = subject.canonicalize(bundle)

        val workspace = requireNotNull(result.workspaces).single()
        assertEquals(OWNER_ID, workspace.id)
        assertEquals(WorkspaceProvenance.CANONICAL_ONLY.name, workspace.provenance)
        assertEquals(null, workspace.sourceContextId)
        assertFalse(workspace.isDeleted)
        assertTrue(result.contexts.single().isDeleted)
        val backlog = requireNotNull(result.workspaceBacklogEntries).single()
        val inbox = requireNotNull(result.workspaceInboxRecords).single()
        assertEquals(BACKLOG_ID, backlog.id)
        assertEquals(OWNER_ID, backlog.workspaceId)
        assertEquals(NOTE_ID, backlog.targetId)
        assertTrue(result.backlogItems.isEmpty())
        assertTrue(result.backlogOrders.isEmpty())
        assertEquals(INBOX_ID, inbox.id)
        assertEquals(OWNER_ID, inbox.workspaceId)
        val types = requireNotNull(result.workspaceCapabilityInstances).map { it.capabilityType }.toSet()
        assertTrue(WorkspaceCapabilityType.BACKLOG.name in types)
        assertTrue(WorkspaceCapabilityType.INBOX.name in types)

        val preferences = requireNotNull(result.workspacePresentationPreferences).single()
        assertEquals(OWNER_ID, preferences.workspaceId)
        assertEquals("DIRECTION", preferences.defaultViewModeName)
        assertFalse(preferences.isAttachmentsExpanded)
        assertFalse(preferences.showCheckboxes)
        assertEquals(10L, preferences.createdAt)
        assertEquals(20L, preferences.updatedAt)
        assertEquals(2L, preferences.version)

        val tag = requireNotNull(result.workspaceTagRefs).single()
        assertEquals(OWNER_ID, tag.workspaceId)
        assertEquals("tag", tag.normalizedTag)
        assertFalse(tag.isDeleted)
        assertTrue(result.contexts.none { !it.isDeleted })
    }

    @Test
    fun `legacy topology is consumed without entering canonical Workspace state`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts =
                        listOf(
                            context(id = OWNER_ID, order = 3),
                            context(id = "child", parentId = OWNER_ID, order = 7),
                        ),
                    backlogItems =
                        listOf(
                            backlogItem(OWNER_ID, "child").copy(itemType = "SUBLIST"),
                        ),
                ),
            )

        assertTrue(
            requireNotNull(result.workspaces).all {
                it.parentWorkspaceId == null && it.workspaceOrder == 0L
            },
        )
        val placements = requireNotNull(result.hierarchyPlacements)
        val root = placements.single { it.targetId == OWNER_ID }
        val child = placements.single { it.targetId == "child" }
        assertEquals(root.id, child.parentPlacementId)
        assertTrue(requireNotNull(result.workspaceBacklogEntries).isEmpty())
    }

    @Test
    fun `irrelevant deleted Context does not require a BACKLOG anchor`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts =
                        listOf(
                            context(OWNER_ID),
                            context("deleted-unused", isDeleted = true),
                        ),
                    notes = listOf(note(NOTE_ID, OWNER_ID)),
                    backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                ),
            )

        val capabilities = requireNotNull(result.workspaceCapabilityInstances)

        assertTrue(
            capabilities.none {
                it.workspaceId == "deleted-unused" &&
                    it.capabilityType == WorkspaceCapabilityType.BACKLOG.name
            },
        )
        assertEquals(
            OWNER_ID,
            requireNotNull(result.workspaceBacklogEntries).single().workspaceId,
        )
    }

    @Test
    fun `deleted legacy BACKLOG owner is preserved as canonical tombstones`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID, isDeleted = true)),
                    notes = listOf(note(NOTE_ID, OWNER_ID)),
                    backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                ),
            )

        val workspace = requireNotNull(result.workspaces).single()
        assertTrue(workspace.isDeleted)

        val capability =
            requireNotNull(result.workspaceCapabilityInstances)
                .single {
                    it.workspaceId == OWNER_ID &&
                        it.capabilityType == WorkspaceCapabilityType.BACKLOG.name
                }
        assertTrue(capability.isDeleted)
        assertEquals(WorkspaceCapabilityState.DISABLED.name, capability.state)

        val backlog = requireNotNull(result.workspaceBacklogEntries).single()
        assertEquals(OWNER_ID, backlog.workspaceId)
        assertEquals(capability.id, backlog.capabilityInstanceId)
        assertTrue(backlog.isDeleted)
    }

    @Test
    fun `legacy inbox order is normalized per Workspace into canonical dense order`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID)),
                    inbox =
                        listOf(
                            inbox(OWNER_ID).copy(
                                id = "late",
                                order = 40L,
                                createdAt = 30L,
                            ),
                            inbox(OWNER_ID).copy(
                                id = "early-b",
                                order = -12L,
                                createdAt = 20L,
                            ),
                            inbox(OWNER_ID).copy(
                                id = "early-a",
                                order = -12L,
                                createdAt = 10L,
                            ),
                        ),
                ),
            )

        val records = requireNotNull(result.workspaceInboxRecords)
        assertEquals(
            mapOf(
                "early-a" to 0L,
                "early-b" to 1L,
                "late" to 2L,
            ),
            records.associate { it.id to it.order },
        )
    }

    @Test
    fun `explicit canonical empty content remains authoritative`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID)),
                    notes = listOf(note(NOTE_ID, OWNER_ID)),
                    backlogItems = listOf(backlogItem(OWNER_ID, NOTE_ID)),
                    inbox = listOf(inbox(OWNER_ID)),
                    workspaceBacklogEntries = emptyList(),
                    workspaceInboxRecords = emptyList(),
                    workspacePresentationPreferences = emptyList(),
                ),
            )

        assertTrue(requireNotNull(result.workspaceBacklogEntries).isEmpty())
        assertTrue(requireNotNull(result.workspaceInboxRecords).isEmpty())
        assertTrue(requireNotNull(result.workspacePresentationPreferences).isEmpty())
    }

    @Test
    fun `legacy inbox sorting becomes typed canonical capability configuration`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    version = 2,
                    exportedAt = 100L,
                    contexts = listOf(context(OWNER_ID)),
                    contextInboxSortingRules =
                        listOf(
                            ContextInboxSortingSnapshot(
                                contextId = OWNER_ID,
                                rulesText = "attachments:type\nbacklog:oldest",
                                updatedAt = 55L,
                            ),
                        ),
                ),
            )

        val capability =
            requireNotNull(result.workspaceCapabilityInstances)
                .single { it.capabilityType == WorkspaceCapabilityType.INBOX_SORTING.name }
        val configuration =
            InboxSortingCapabilityConfigurationCodec.decode(
                capability.configurationVersion,
                capability.configuration,
            )

        assertEquals(2, configuration.rules.size)
        assertEquals(WorkspaceSortingTarget.CONNECTIONS, configuration.rules[0].target)
        assertEquals(WorkspaceSortingMode.TYPE, configuration.rules[0].mode)
        assertEquals(WorkspaceSortingTarget.BACKLOG, configuration.rules[1].target)
        assertEquals(WorkspaceSortingMode.OLDEST, configuration.rules[1].mode)
        assertTrue(capability.updatedAt >= 55L)
    }

    @Test
    fun `historical non reserved sys id remains ordinary while exact system shell is removed`() {
        val exactSystemId = SystemContexts.INBOX.raw
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context("sys_custom"), context(exactSystemId)),
                ),
            )

        assertNotNull(result.workspaces?.single { it.id == "sys_custom" })
        assertTrue(result.contexts.single { it.id == "sys_custom" }.isDeleted)
        assertTrue(result.contexts.none { it.id == exactSystemId })
        assertNotNull(result.workspaces?.single { it.id == exactSystemId })
    }

    @Test
    fun `legacy execution log Restore ingress becomes canonical Workspace-owned log`() {
        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID)),
                    logs = listOf(legacyLog("legacy-log", OWNER_ID)),
                ),
            )

        val canonicalLog = requireNotNull(result.canonicalExecutionLogs).single()
        assertEquals("legacy-log", canonicalLog.id)
        assertEquals(OWNER_ID, canonicalLog.workspaceId)
        assertEquals("Legacy execution history", canonicalLog.description)
        assertTrue(result.logs.isEmpty())
        assertTrue(result.contexts.none { !it.isDeleted })
        assertEquals(OWNER_ID, requireNotNull(result.workspaces).single().id)
    }

    @Test
    fun `canonical execution log payload is authoritative over legacy Restore copy`() {
        val canonical =
            CanonicalExecutionLogSnapshot(
                id = "canonical-log",
                workspaceId = "workspace",
                timestamp = 40L,
                type = "COMMENT",
                description = "Canonical",
                details = null,
                updatedAt = 50L,
                version = 3L,
                isDeleted = false,
            )

        val result =
            subject.canonicalize(
                SnapshotBundle(
                    logs = listOf(legacyLog("legacy-copy", "missing-context")),
                    canonicalExecutionLogs = listOf(canonical),
                ),
            )

        assertEquals(listOf(canonical), result.canonicalExecutionLogs)
        assertTrue(result.logs.isEmpty())
    }

    @Test
    fun `legacy execution log Restore ingress fails without ownership evidence`() {
        val failure =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        logs = listOf(legacyLog("orphan", "missing-context")),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `legacy execution log Restore ingress rejects conflicting Workspace provenance`() {
        val failure =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        contexts = listOf(context(OWNER_ID, isDeleted = true)),
                        workspaces =
                            listOf(
                                workspace(
                                    id = OWNER_ID,
                                    provenance = WorkspaceProvenance.STANDALONE.name,
                                    sourceContextId = null,
                                ),
                            ),
                        logs = listOf(legacyLog("conflict", OWNER_ID)),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `legacy Tactical owner converges without rewriting carrier metadata`() {
        val mission =
            tacticalMission(id = 801L, ownerId = OWNER_ID).copy(
                sourceContextId = OWNER_ID,
                sourceBacklogItemId = "backlog-source",
                activitySlotContextId = "activity-slot",
                linkedProjectIds = listOf(OWNER_ID),
                linkedAttachmentIds = listOf("attachment-1"),
                missionStreamId = "stream-1",
                iterationId = "cycle-1",
                carriedFromMissionId = 800L,
                orderInSlot = 7L,
            )

        val result =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID)),
                    tacticalMissions = listOf(mission),
                ),
            )

        assertEquals(listOf(mission), result.tacticalMissions)
        assertEquals(
            WorkspaceProvenance.CANONICAL_ONLY.name,
            requireNotNull(result.workspaces)
                .single { it.id == OWNER_ID }.provenance,
        )
        assertTrue(result.contexts.single { it.id == OWNER_ID }.isDeleted)
    }

    @Test
    fun `canonical Tactical owners survive while missing and deleted owners are neutralized`() {
        val admittedStandalone = tacticalMission(811L, OWNER_ID)
        val admittedCanonical = tacticalMission(812L, "canonical-owner")
        val missing = tacticalMission(813L, "missing-owner")
        val deleted = tacticalMission(814L, "deleted-owner")
        val ownerless = tacticalMission(815L, null)

        val result =
            subject.canonicalize(
                SnapshotBundle(
                    workspaces = listOf(
                        workspace(
                            OWNER_ID,
                            WorkspaceProvenance.STANDALONE.name,
                            null,
                        ),
                        workspace(
                            "canonical-owner",
                            WorkspaceProvenance.CANONICAL_ONLY.name,
                            null,
                        ),
                        workspace(
                            "deleted-owner",
                            WorkspaceProvenance.CANONICAL_ONLY.name,
                            null,
                        ).copy(isDeleted = true),
                    ),
                    tacticalMissions = listOf(
                        admittedStandalone,
                        admittedCanonical,
                        missing,
                        deleted,
                        ownerless,
                    ),
                ),
            )

        assertEquals(
            listOf(
                admittedStandalone,
                admittedCanonical,
                missing.copy(projectId = null),
                deleted.copy(projectId = null),
                ownerless,
            ),
            result.tacticalMissions,
        )
    }

    @Test
    fun `retired Context requires canonical only provenance for Tactical restore`() {
        val admitted =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID, isDeleted = true)),
                    workspaces = listOf(
                        workspace(
                            OWNER_ID,
                            WorkspaceProvenance.CANONICAL_ONLY.name,
                            null,
                        ),
                    ),
                    tacticalMissions = listOf(tacticalMission(821L, OWNER_ID)),
                ),
            )

        assertEquals(OWNER_ID, admitted.tacticalMissions.single().projectId)

        val rejected =
            subject.canonicalize(
                SnapshotBundle(
                    contexts = listOf(context(OWNER_ID, isDeleted = true)),
                    workspaces = listOf(
                        workspace(
                            OWNER_ID,
                            WorkspaceProvenance.CONTEXT_BACKED.name,
                            OWNER_ID,
                        ),
                    ),
                    tacticalMissions = listOf(tacticalMission(822L, OWNER_ID)),
                ),
            )

        assertEquals(null, rejected.tacticalMissions.single().projectId)
    }

    @Test
    fun `ambiguous Tactical Workspace evidence fails closed during restore`() {
        val candidate =
            workspace(
                OWNER_ID,
                WorkspaceProvenance.STANDALONE.name,
                null,
            )

        val failure =
            runCatching {
                subject.canonicalize(
                    SnapshotBundle(
                        workspaces = listOf(candidate, candidate.copy(version = 2L)),
                        tacticalMissions = listOf(tacticalMission(831L, OWNER_ID)),
                    ),
                )
            }.exceptionOrNull()

        assertTrue(
            "Ambiguous Workspace evidence must reject restore: $failure",
            failure is IllegalArgumentException,
        )
    }

    private fun tacticalMission(
        id: Long,
        ownerId: String?,
    ) =
        TacticalMissionSnapshot(
            id = id,
            title = "Mission $id",
            description = "Preserve description",
            startTime = 10L,
            deadline = 100L,
            status = "ACTIVE",
            priority = "MEDIUM",
            projectId = ownerId,
            linkedProjectIds = listOf("related-project"),
            linkedAttachmentIds = listOf("related-attachment"),
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = 3L,
            version = 4L,
        )

    private fun context(
        id: String,
        isDeleted: Boolean = false,
        parentId: String? = null,
        order: Int = 0,
    ) =
        ContextSnapshot(
            id = id,
            name = id,
            parentId = parentId,
            description = "description",
            createdAt = 10L,
            updatedAt = 20L,
            isExpanded = false,
            isDeleted = isDeleted,
            version = 2L,
            tags = listOf("tag"),
            relatedLinks = emptyList(),
            order = order,
            isAttachmentsExpanded = false,
            defaultViewModeName = "DIRECTION",
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = 0L,
            valueImportance = 0,
            valueImpact = 0,
            effort = 0,
            cost = 0,
            risk = 0,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0.0,
            displayScore = 0.0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null,
        )

    private fun note(id: String, ownerId: String) =
        LegacyNoteSnapshot(
            id = id,
            contextId = ownerId,
            title = "Note",
            content = "Body",
            createdAt = 11L,
            updatedAt = 21L,
            isDeleted = false,
            version = 3L,
        )

    private fun backlogItem(ownerId: String, targetId: String) =
        BacklogItemSnapshot(
            id = BACKLOG_ID,
            contextId = ownerId,
            itemType = "NOTE",
            entityId = targetId,
            order = 7L,
            updatedAt = 30L,
            version = 4L,
            isDeleted = false,
        )

    private fun inbox(ownerId: String) =
        InboxRecordSnapshot(
            id = INBOX_ID,
            contextId = ownerId,
            text = "Inbox",
            createdAt = 12L,
            order = -12L,
            updatedAt = 31L,
            hideInOwnerInbox = false,
            version = 5L,
            isDeleted = false,
        )

    private fun legacyLog(id: String, ownerId: String) =
        ContextLogSnapshot(
            id = id,
            contextId = ownerId,
            timestamp = 30L,
            type = "COMMENT",
            description = "Legacy execution history",
            details = "details",
            updatedAt = 40L,
            version = 2L,
            isDeleted = false,
        )

    private fun workspace(
        id: String,
        provenance: String,
        sourceContextId: String?,
    ) =
        WorkspaceSnapshot(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = provenance,
            sourceContextId = sourceContextId,
        )

    private companion object {
        const val OWNER_ID = "ordinary-owner"
        const val NOTE_ID = "note-id"
        const val BACKLOG_ID = "backlog-id"
        const val INBOX_ID = "inbox-id"
    }
}
