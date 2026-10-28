package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.hasCompleteCanonicalHierarchyTransport
import com.romankozak.forwardappmobile.core.data.models.sync.requireSupportedHierarchyFormat
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedMainBeaconTopology
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection

class SnapshotBundleSelectiveImportFilter {
    fun filter(
        source: SnapshotBundle,
        selection: WorkspaceSelectiveImportSelection,
    ): SnapshotBundle {
        require(source.planningCommitmentRelations.isNullOrEmpty()) {
            "Selective Planning composition import has no endpoint-selection closure yet; use full Restore or Merge."
        }
        require(source.managedSubjectConversions.isNullOrEmpty()) {
            "Selective import of semantic conversion history is not yet supported; use full Restore or Merge."
        }
        source.requireSupportedHierarchyFormat()

        // selectedContextIds is retained as a compatibility/UI contract name,
        // but current selective-import authority is canonical Workspace identity.
        val selectedWorkspaceIds =
            source.workspaces
                .orEmpty()
                .asSequence()
                .map { workspace -> workspace.id }
                .filter { workspaceId -> workspaceId in selection.selectedContextIds }
                .toCollection(linkedSetOf())

        val filteredGoals = source.goals.filter { goal -> goal.id in selection.selectedGoalIds }
        val filteredDocuments =
            source.documents.filter { document ->
                document.id in selection.selectedDocumentIds &&
                    (document.contextId == null || document.contextId in selectedWorkspaceIds)
            }
        val filteredChecklists =
            source.checklists.filter { checklist ->
                checklist.id in selection.selectedChecklistIds &&
                    (checklist.contextId == null || checklist.contextId in selectedWorkspaceIds)
            }
        val filteredLinks = source.linkItemEntities.filter { link -> link.id in selection.selectedLinkItemIds }
        val executionLogOwnerContexts = source.contextBackedExecutionLogOwnerContexts()
        val selectedExecutionLogWorkspaceIds =
            executionLogOwnerContexts
                .filterValues { contextId -> contextId in selectedWorkspaceIds }
                .keys
        val filteredCanonicalExecutionLogs =
            source.canonicalExecutionLogs?.filter { log ->
                log.id in selection.selectedContextLogIds &&
                    log.workspaceId in selectedExecutionLogWorkspaceIds
            }
        val filteredScripts = source.scripts.filter { script -> script.id in selection.selectedScriptIds }
        val filteredAttachments = source.attachments.filter { attachment -> attachment.id in selection.selectedAttachmentIds }
        val validAttachmentIds = filteredAttachments.mapTo(linkedSetOf()) { attachment -> attachment.id }
        val filteredDayPlans = source.dayPlans
        val validDayPlanIds = filteredDayPlans.mapTo(linkedSetOf()) { plan -> plan.id }
        val filteredMainBeacons =
            source.mainBeacons.map { it.withoutEmbeddedMainBeaconTopology() }
        val validMainBeaconIds = filteredMainBeacons.mapTo(linkedSetOf()) { beacon -> beacon.id }

        // Canonical Selective Import may retain an operational owner ref
        // only when the corresponding live Workspace is explicitly selected.
        // A historical Context-shaped ref without canonical transport
        // evidence is not automatically promoted by matching ids.
        val selectedOwnerWorkspaces =
            source.workspaces.orEmpty()
                .filter { it.id in selectedWorkspaceIds }

        require(
            selectedOwnerWorkspaces.map { it.id }.distinct().size ==
                selectedOwnerWorkspaces.size,
        ) {
            "Selective MainBeacon owners contain duplicate Workspace identities"
        }

        val selectedOwnersById =
            selectedOwnerWorkspaces.associateBy { it.id }

        val selectedMainBeaconOwnerRefs =
            if (!source.hasCompleteCanonicalHierarchyTransport()) {
                emptyList()
            } else {
                source.mainBeaconContextCrossRefs.filter { ref ->
                    if (
                        ref.beaconId !in validMainBeaconIds ||
                        ref.contextId !in selectedWorkspaceIds
                    ) {
                        false
                    } else {
                        val workspace =
                            requireNotNull(selectedOwnersById[ref.contextId]) {
                                "Selective MainBeacon owner has no selected Workspace: " +
                                    ref.contextId
                            }

                        val canonical =
                            workspace.provenance == "CANONICAL_ONLY" &&
                                workspace.sourceContextId == null
                        val standalone =
                            workspace.provenance == "STANDALONE" &&
                                workspace.sourceContextId == null
                        val contextBacked =
                            workspace.provenance == "CONTEXT_BACKED" &&
                                workspace.sourceContextId == workspace.id

                        require(
                            !workspace.isDeleted &&
                                (canonical || standalone || contextBacked),
                        ) {
                            "Selective MainBeacon owner has invalid Workspace provenance: " +
                                ref.contextId
                        }

                        true
                    }
                }
            }

        val duplicateSelectedOwner =
            selectedMainBeaconOwnerRefs
                .groupingBy { it.beaconId to it.contextId }
                .eachCount()
                .entries
                .firstOrNull { it.value > 1 }

        require(duplicateSelectedOwner == null) {
            "Selective MainBeacon ownership contains duplicate logical association " +
                "${duplicateSelectedOwner?.key}"
        }

        val filtered = source.copy(
            // Canonical Planning has no selective-import selection surface in V3.3.
            // Selecting an Orientation must never implicitly import its Planning participation.
            planningFormatVersion = null,
            planningScopes = null,
            planningCommitments = null,
            planningReferences = null,
            planningCommitmentRelations = null,
            dayPlanningScopeExtensions = null,
            tacticalPlanningScopeExtensions = null,
            strategicPlanningScopeExtensions = null,
            hierarchyFormatVersion = null,
            // Ordinary Context persistence is extinct on current selective import.
            // Historical Context-shaped evidence remains Restore-only.
            contexts = emptyList(),
            // Historical links are Restore-only evidence and have no
            // schema-180 selective-import persistence owner.
            contextParentLinks = emptyList(),
            goals = filteredGoals,
            // Legacy BACKLOG is neither selectable nor importable after the
            // canonical Workspace placement cutover. Canonical placement
            // selection and dependency closure are applied below.
            backlogItems = emptyList(),
            backlogOrders = emptyList(),
            documents = filteredDocuments,
            checklists = filteredChecklists,
            checklistItems =
                source.checklistItems.filter { item ->
                    item.checklistId in selection.selectedChecklistIds
                },
            linkItemEntities = filteredLinks,
            inbox = emptyList(),
            // Legacy Context logs are not an authority after EXECUTION_LOG cutover.
            // selectedContextLogIds are presentation-level stable row ids only.
            logs = emptyList(),
            canonicalExecutionLogs = filteredCanonicalExecutionLogs,
            workspaceDirectionEntries = null,
            scripts = filteredScripts,
            attachments = filteredAttachments,
            dayPlans = filteredDayPlans,
            dayFocusItems =
                source.dayFocusItems.filter { item ->
                    item.dayPlanId in validDayPlanIds
                },
            dayTasks =
                source.dayTasks.filter { task ->
                    task.dayPlanId in validDayPlanIds
                },
            dailyMetrics =
                source.dailyMetrics.filter { metric ->
                    metric.dayPlanId in validDayPlanIds
                },
            activityRecords =
                source.activityRecords.filter { record ->
                    record.id in selection.selectedActivityRecordIds
                },
            mainBeacons = filteredMainBeacons,
            // Logical compatibility property, physically Workspace-owned.
            // Only explicitly selected, provenance-validated canonical refs.
            mainBeaconContextCrossRefs = selectedMainBeaconOwnerRefs,
            mainBeaconAttachmentCrossRefs =
                source.mainBeaconAttachmentCrossRefs.filter { crossRef ->
                    crossRef.beaconId in validMainBeaconIds && crossRef.attachmentId in validAttachmentIds
                },
            mainBeaconLevelStatuses =
                source.mainBeaconLevelStatuses.filter { status ->
                    status.mainBeaconId in validMainBeaconIds
                },
            lifeManagementLevelStatuses = source.lifeManagementLevelStatuses,
            dayManagementRuntimeState = source.dayManagementRuntimeState,
            notes = emptyList(),
            musicNotes = emptyList(),
            systemApps = emptyList(),
            recentProjectEntries = emptyList(),
            conversations = emptyList(),
            chatMessages = emptyList(),
            conversationFolders = emptyList(),
            reminders = emptyList(),
            recurringTasks = emptyList(),
            tacticalMissions = emptyList(),
            tacticalMissionAttachments = emptyList(),
            tacticalIterations = emptyList(),
            missionStreams = emptyList(),
            tacticalActivitySlots = emptyList(),
            arcQuests = emptyList(),
            aiEvents = emptyList(),
            aiInsights = emptyList(),
            lifeSystemStates = source.lifeSystemStates,
            contextRoleProfiles = emptyList(),
            contextRoleProfileItems = emptyList(),
            contextConfigurations = emptyList(),
            projectStructureItems = emptyList(),
            contextInboxSortingRules = emptyList(),
            workspaceProblems = null,
            workspaceProblemWorkspaceRefs = null,
            workspaceProblemAttachmentRefs = null,
            workspaceInboxRecords = null,
            workspaceConnections = null,
            workspaceBacklogEntries = null,
            // Presentation preferences have no selective-import selection surface.
            // Never inherit the source-wide canonical collection implicitly.
            workspacePresentationPreferences = null,
            focusContextIntervals = emptyList(),
            userStateIntervals = emptyList(),
            // Never inherit the full source hierarchy. Selective H1,
            // GroupScope, and linked-presentation provenance are reintroduced
            // only through the exact occurrence closure below.
            hierarchyPlacements = null,
            hierarchyPlacementGroupScopes = null,
            groupScopeCoverage = null,
            hierarchyPlacementLinkedAppearances = null,
            bigPicturePresentation = null,
        )
        val closed =
            filtered
                .withCanonicalBacklogSelectiveClosure(
                    source = source,
                    selectedIds = selection.selectedWorkspaceBacklogEntryIds,
                )
                .withCanonicalHierarchySelectiveClosure(
                    source = source,
                    selectedContextIds = selection.selectedContextIds,
                )

        val presentationClosed =
            closed.withCanonicalBigPictureSelectiveClosure(source)

        return presentationClosed.copy(
            hierarchyFormatVersion =
                source.hierarchyFormatVersion?.takeIf {
                    presentationClosed.hasCompleteCanonicalHierarchyTransport()
                },
        ).withSemanticTagsFrom(source)
    }
}
