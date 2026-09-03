@file:Suppress("WildcardImport")

package com.romankozak.forwardappmobile.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceBacklogEntrySnapshot
import com.romankozak.forwardappmobile.shared.core.domain.orientation.*
import com.romankozak.forwardappmobile.shared.core.domain.workspace.validateBacklogContract
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogEntry
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef

internal fun SnapshotBundle.withCanonicalBacklogSelectiveClosure(
    source: SnapshotBundle,
    selectedIds: Set<String>,
): SnapshotBundle {
    if (selectedIds.isEmpty()) return copy(workspaceBacklogEntries = null)

    val sourceEntries = requireNotNull(source.workspaceBacklogEntries) {
        "Canonical BACKLOG selection requires workspaceBacklogEntries in the source snapshot."
    }
    val sourceById = sourceEntries.associateBy { it.id }
    val missingIds = selectedIds - sourceById.keys
    require(missingIds.isEmpty()) {
        "Canonical BACKLOG selection references missing placement ids: ${missingIds.sorted().joinToString()}"
    }
    val selectedEntries = sourceEntries.filter { it.id in selectedIds }
    require(selectedEntries.isNotEmpty()) {
        "Canonical BACKLOG selective import must not emit an authoritative empty placement collection."
    }
    val backlogViolations = validateBacklogContract(selectedEntries.map { it.toModel() })
    require(backlogViolations.isEmpty()) { backlogViolations.first().message }

    val sourceWorkspaces = requireNotNull(source.workspaces) {
        "Canonical BACKLOG selection requires canonical Workspaces in the source snapshot."
    }
    val sourceCapabilities = requireNotNull(source.workspaceCapabilityInstances) {
        "Canonical BACKLOG selection requires Workspace capability instances in the source snapshot."
    }
    val workspaceById = sourceWorkspaces.associateBy { it.id }
    val capabilityById = sourceCapabilities.associateBy { it.id }
    selectedEntries.forEach { entry ->
        val owner = requireNotNull(workspaceById[entry.workspaceId]) {
            "Canonical BACKLOG placement ${entry.id} references missing Workspace ${entry.workspaceId}."
        }
        require(owner.isDeleted.not() || entry.isDeleted) {
            "Live canonical BACKLOG placement ${entry.id} belongs to a deleted Workspace."
        }
        val capability = requireNotNull(capabilityById[entry.capabilityInstanceId]) {
            "Canonical BACKLOG placement ${entry.id} references missing capability ${entry.capabilityInstanceId}."
        }
        require(capability.workspaceId == entry.workspaceId) {
            "Canonical BACKLOG placement ${entry.id} capability belongs to another Workspace."
        }
        require(capability.capabilityType == WorkspaceCapabilityType.BACKLOG.name) {
            "Canonical BACKLOG placement ${entry.id} must reference a BACKLOG capability."
        }
    }

    val targetDependencies = selectedEntries.canonicalBacklogTargetDependencies()
    val requiredWorkspaceIds =
        collectWorkspaceClosure(
            roots = selectedEntries.mapTo(linkedSetOf()) { it.workspaceId } + targetDependencies.workspaceIds,
            workspaceById = workspaceById,
        )
    val selectedWorkspaces = sourceWorkspaces.filter { it.id in requiredWorkspaceIds }
    val selectedCapabilities =
        sourceCapabilities.filter { capability ->
            selectedEntries.any { it.capabilityInstanceId == capability.id }
        }

    requireLiveTargets(source, targetDependencies)

    val orientationIds = targetDependencies.orientationIds
    val selectedSubjects = requireCanonicalRows(source.managedSubjects, "managedSubjects")
        .filter { it.id in orientationIds }
    val selectedOrientations = requireCanonicalRows(source.orientations, "orientations")
        .filter { it.subjectId in orientationIds }
    val selectedAssessments = requireCanonicalRows(source.orientationAssessments, "orientationAssessments")
        .filter { it.orientationId in orientationIds }
    val requiredRevisionIds = selectedAssessments.mapTo(hashSetOf()) { it.revisionId }
    val selectedRevisions =
        requireCanonicalRows(source.orientationAssessmentRevisions, "orientationAssessmentRevisions")
            .filter { it.id in requiredRevisionIds }
    val selectedMappings = requireCanonicalRows(source.legacySubjectMappings, "legacySubjectMappings")
        .filter { it.subjectId in orientationIds }

    val result =
        copy(
            backlogItems = emptyList(),
            backlogOrders = emptyList(),
            notes = mergeById(notes, source.notes.filter { it.id in targetDependencies.legacyNoteIds }) { it.id },
            documents = mergeById(documents, source.documents.filter { it.id in targetDependencies.documentIds }) { it.id },
            musicNotes = mergeById(musicNotes, source.musicNotes.filter { it.id in targetDependencies.musicNoteIds }) { it.id },
            checklists = mergeById(checklists, source.checklists.filter { it.id in targetDependencies.checklistIds }) { it.id },
            checklistItems =
                mergeById(
                    checklistItems,
                    source.checklistItems.filter { it.checklistId in targetDependencies.checklistIds },
                ) { it.id },
            linkItemEntities =
                mergeById(
                    linkItemEntities,
                    source.linkItemEntities.filter { it.id in targetDependencies.linkItemIds },
                ) { it.id },
            managedSubjects = selectedSubjects,
            orientations = selectedOrientations,
            aspects = emptyList(),
            orientationAssessments = selectedAssessments,
            orientationAssessmentRevisions = selectedRevisions,
            legacySubjectMappings = selectedMappings,
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces = selectedWorkspaces,
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = selectedCapabilities,
            savedOrientationViews = emptyList(),
            workspaceBacklogEntries = selectedEntries,
        )

    val violations = validateCanonicalOrientationReferences(result.toCanonicalOrientationValidationGraph())
    require(violations.isEmpty()) { violations.first().message }
    return result
}

private fun collectWorkspaceClosure(
    roots: Set<String>,
    workspaceById: Map<String, WorkspaceEntity>,
): Set<String> {
    val required = linkedSetOf<String>()
    roots.forEach { root ->
        var current: String? = root
        while (current != null && required.add(current)) {
            val workspace = requireNotNull(workspaceById[current]) {
                "Canonical BACKLOG dependency references missing Workspace $current."
            }
            current = workspace.parentWorkspaceId
        }
    }
    return required
}

private fun requireLiveTargets(
    source: SnapshotBundle,
    dependencies: CanonicalBacklogTargetDependencies,
) {
    dependencies.orientationIds.forEach { id ->
        val subject = requireNotNull(source.managedSubjects.orEmpty().firstOrNull { it.id == id }) {
            "Canonical BACKLOG target Orientation subject $id is missing."
        }
        require(!subject.isDeleted && subject.subjectType == ManagedSubjectType.ORIENTATION.name) {
            "Canonical BACKLOG target $id is not a live Orientation subject."
        }
        require(source.orientations.orEmpty().any { it.subjectId == id }) {
            "Canonical BACKLOG target Orientation node $id is missing."
        }
    }
    dependencies.workspaceIds.forEach { id ->
        require(source.workspaces.orEmpty().any { it.id == id && !it.isDeleted }) {
            "Canonical BACKLOG target Workspace $id is missing or deleted."
        }
    }
    dependencies.linkItemIds.requireLive("LinkItem", source.linkItemEntities, { it.id }, { it.isDeleted })
    dependencies.legacyNoteIds.requireLive("legacy Note", source.notes, { it.id }, { it.isDeleted })
    dependencies.documentIds.requireLive("document", source.documents, { it.id }, { it.isDeleted })
    dependencies.checklistIds.requireLive("Checklist", source.checklists, { it.id }, { it.isDeleted })
    dependencies.musicNoteIds.requireLive("MusicNote", source.musicNotes, { it.id }, { it.isDeleted })
}

private fun <T> Set<String>.requireLive(
    label: String,
    rows: List<T>,
    id: (T) -> String,
    deleted: (T) -> Boolean,
) {
    forEach { requiredId ->
        require(rows.any { id(it) == requiredId && !deleted(it) }) {
            "Canonical BACKLOG target $label $requiredId is missing or deleted."
        }
    }
}

private fun <T> requireCanonicalRows(rows: List<T>?, fieldName: String): List<T> =
    requireNotNull(rows) {
        "Canonical BACKLOG Orientation dependency closure requires $fieldName in the source snapshot."
    }

private fun <T> mergeById(base: List<T>, dependencies: List<T>, id: (T) -> String): List<T> =
    (base + dependencies).associateBy(id).values.toList()

private fun WorkspaceBacklogEntrySnapshot.toModel() =
    WorkspaceBacklogEntry(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = null,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        target = WorkspaceBacklogTargetRef(WorkspaceBacklogTargetKind.valueOf(targetKind), targetId),
        order = order,
    )

private fun SnapshotBundle.toCanonicalOrientationValidationGraph(): CanonicalOrientationValidationGraph {
    val gson = Gson()
    return CanonicalOrientationValidationGraph(
        subjects = requireNotNull(managedSubjects).map {
            CanonicalSubjectReference(it.id, ManagedSubjectType.valueOf(it.subjectType))
        },
        orientations = requireNotNull(orientations).map {
            CanonicalOrientationReference(it.subjectId, OrientationKind.valueOf(it.kind))
        },
        aspects = requireNotNull(aspects).map { CanonicalAspectReference(it.subjectId, it.parentAspectId) },
        assessments = requireNotNull(orientationAssessments).map {
            CanonicalCurrentAssessmentReference(it.orientationId, it.revisionId, it.toAssessment())
        },
        revisions = requireNotNull(orientationAssessmentRevisions).map {
            CanonicalAssessmentRevisionReference(
                id = it.id,
                orientationId = it.orientationId,
                assessment = gson.fromJson(it.assessmentJson, OrientationAssessment::class.java),
            )
        },
        mappings = requireNotNull(legacySubjectMappings).map {
            CanonicalLegacyMappingReference(it.id, it.subjectId)
        },
        relations = requireNotNull(orientationRelations).map { it.toModel() },
        aspectRefs = requireNotNull(aspectOrientationRefs).map { it.toModel() },
        workspaces = requireNotNull(workspaces).map { CanonicalWorkspaceReference(it.id, it.parentWorkspaceId) },
        bindings = requireNotNull(workspaceBindings).map { it.toModel() },
        capabilities = requireNotNull(workspaceCapabilityInstances).map { it.toModel() },
        savedViews = requireNotNull(savedOrientationViews).map {
            CanonicalSavedViewReference(it.id, it.filterAstVersion)
        },
    )
}

private fun OrientationAssessmentEntity.toAssessment() =
    OrientationAssessment(
        importance = AxisAssessment(importanceValue, ValueOrigin.valueOf(importanceOrigin)),
        impact = AxisAssessment(impactValue, ValueOrigin.valueOf(impactOrigin)),
        breadth = AxisAssessment(breadthValue, ValueOrigin.valueOf(breadthOrigin)),
        expectedSpan = AxisAssessment(expectedSpanValue, ValueOrigin.valueOf(expectedSpanOrigin)),
        targetWindow = AxisAssessment(targetWindowValue, ValueOrigin.valueOf(targetWindowOrigin)),
        attentionTier = AxisAssessment(attentionTierValue, ValueOrigin.valueOf(attentionTierOrigin)),
        commitment = AxisAssessment(commitmentValue, ValueOrigin.valueOf(commitmentOrigin)),
        confidence = AxisAssessment(confidenceValue, ValueOrigin.valueOf(confidenceOrigin)),
    )

private fun OrientationRelationEntity.toModel() =
    OrientationRelation(
        id, createdAt, updatedAt, syncedAt, isDeleted, version, fromOrientationId, toOrientationId,
        OrientationRelationType.valueOf(relationType), relationOrder,
    )

private fun AspectOrientationRefEntity.toModel() =
    AspectOrientationRef(
        id, createdAt, updatedAt, syncedAt, isDeleted, version, aspectId, orientationId,
        AspectOrientationRelationType.valueOf(relationType), isPrimary, refOrder,
    )

private fun WorkspaceBindingEntity.toModel() =
    WorkspaceBinding(
        id, createdAt, updatedAt, syncedAt, isDeleted, version, workspaceId, subjectId,
        WorkspaceBindingType.valueOf(bindingType), isPrimary, bindingOrder,
    )

private fun WorkspaceCapabilityInstanceEntity.toModel() =
    WorkspaceCapabilityInstance(
        id, createdAt, updatedAt, syncedAt, isDeleted, version, workspaceId,
        WorkspaceCapabilityType.valueOf(capabilityType), instanceKey, capabilityOrder,
        WorkspaceCapabilityState.valueOf(state), configurationVersion, configuration,
    )
