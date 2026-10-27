package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticEntityLifecycleCoordinator
import com.romankozak.forwardappmobile.data.orientation.genericDeletableOrientationKinds
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.isCanonicalWorkspaceOwner
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BigPictureNode
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

data class BigPictureSourceDeleteTarget(val ref: CanonicalEntityRef, val title: String, val version: Long) {
    val classLabel: String get() = when (ref) {
        is CanonicalEntityRef.Orientation -> "орієнтир"
        is CanonicalEntityRef.Aspect -> "аспект"
        is CanonicalEntityRef.Workspace -> "робочий простір"
    }
    val actionLabel: String get() = "Видалити $classLabel…"
    val confirmationTitle: String get() = "Видалити $classLabel «$title»?"
    val confirmationText: String get() = if (ref is CanonicalEntityRef.Workspace)
        "Буде видалено сам робочий простір та належні йому операційні дані. Усі його появи в Navigation також зникнуть. Пов’язані семантичні сутності не видаляються."
    else "Буде видалено саму семантичну сутність ($classLabel). Усі її появи в Navigation також зникнуть. Пов’язані робочі простори та інші сутності не видаляються."
}

/** Read-only capability resolution. Lifecycle commands revalidate the captured version. */
internal fun sourceDeletionTargets(
    subjects: List<ManagedSubjectEntity>, orientations: List<OrientationEntity>, aspects: List<AspectEntity>,
    mappings: List<LegacySubjectMappingEntity>, workspaces: List<WorkspaceEntity>,
): Map<CanonicalEntityRef, BigPictureSourceDeleteTarget> {
    val mapped = mappings.mapTo(hashSetOf()) { it.subjectId }
    val kinds = orientations.associate { it.subjectId to it.kind }
    val aspectIds = aspects.mapTo(hashSetOf()) { it.subjectId }
    val allowedKinds = genericDeletableOrientationKinds.mapTo(hashSetOf(), OrientationKind::name)
    return buildMap {
        subjects.filter { !it.isDeleted && it.id !in mapped }.forEach { subject ->
            val ref = when {
                subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                    kinds[subject.id] in allowedKinds -> CanonicalEntityRef.Orientation(subject.id)
                subject.subjectType == ManagedSubjectType.ASPECT.name && subject.id in aspectIds -> CanonicalEntityRef.Aspect(subject.id)
                else -> null
            }
            if (ref != null) put(ref, BigPictureSourceDeleteTarget(ref, subject.title, subject.version))
        }
        workspaces.filter { !it.isDeleted && it.isCanonicalWorkspaceOwner() && !SystemContexts.isSystem(ContextId(it.id)) }
            .forEach { workspace ->
                val ref = CanonicalEntityRef.Workspace(workspace.id)
                put(ref, BigPictureSourceDeleteTarget(ref, workspace.nameOverride?.trim()?.takeIf { it.isNotEmpty() } ?: workspace.id, workspace.version))
            }
    }
}

internal fun sourceDeleteTarget(node: BigPictureNode, targets: Map<CanonicalEntityRef, BigPictureSourceDeleteTarget>): BigPictureSourceDeleteTarget? {
    val ref = node.entity?.ref ?: return null
    val type = if (ref is CanonicalEntityRef.Workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT
    if (node.placement.isDeleted || node.placement.target.type != type || node.placement.target.id != ref.id) return null
    return targets[ref]
}

internal fun sourceDeleteMenuTarget(node: H1TypedNode, target: BigPictureSourceDeleteTarget?): BigPictureSourceDeleteTarget? =
    target.takeIf { node is H1TypedNode.Occurrence }

/** Big Picture consumes lifecycle; there are no structural writes or source DAO deletions here. */
@Singleton
class BigPictureTargetActions @Inject constructor(
    private val database: AppDatabase,
    private val semantics: CanonicalSemanticEntityLifecycleCoordinator,
    private val workspaces: CanonicalWorkspaceRepository,
) {
    fun observeTargets() = database.invalidationTracker.createFlow(
        "managed_subjects", "orientations", "aspects", "legacy_subject_mappings", "workspaces", emitInitialState = true,
    ).map {
        database.withTransaction {
            val dao = database.orientationDao()
            sourceDeletionTargets(dao.getAllManagedSubjects(), dao.getAllOrientations(), dao.getAllAspects(),
                dao.getAllLegacyMappings(), database.workspaceDao().getAll())
        }
    }

    suspend fun deleteSource(target: BigPictureSourceDeleteTarget) {
        when (val ref = target.ref) {
            is CanonicalEntityRef.Workspace -> workspaces.tombstone(ref.id, expectedVersion = target.version)
            is CanonicalEntityRef.Orientation -> semantics.tombstone(ref.id, ManagedSubjectType.ORIENTATION, target.version)
            is CanonicalEntityRef.Aspect -> semantics.tombstone(ref.id, ManagedSubjectType.ASPECT, target.version)
        }
    }
}
