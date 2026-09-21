package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind

/**
 * Read-only H2 projection of supported Canonical V1 visible appearance
 * occurrences. occurrenceKey is transitional migration identity only.
 */
data class CanonicalV1HierarchySnapshot(
    val hierarchyId: HierarchyId = HierarchyId.GENERAL,
    val occurrences: List<CanonicalV1HierarchyOccurrence>,
    val diagnostics: List<CanonicalV1HierarchyDiagnostic> = emptyList(),
)

data class CanonicalV1HierarchyOccurrence(
    val occurrenceKey: String,
    val target: HierarchyTargetRef,
    val parentOccurrenceKey: String?,
    val siblingOrder: Long,
    val primaryEvidence: CanonicalV1PrimaryEvidence,
    val sourceAuthority: CanonicalV1HierarchySourceAuthority,
    val placementKind: PlacementKind,
    val rootGroupScope: CanonicalV1RootGroupScope? = null,
)

enum class CanonicalV1RootGroupScopeKind {
    GROUP,
    NO_GROUP,
}

/**
 * Explicit provenance for a root MANAGED_SUBJECT occurrence.
 *
 * null occurrence.rootGroupScope means this occurrence is not a synthetic
 * Group/NoGroup root. GROUP carries the canonical Group ManagedSubject id.
 */
data class CanonicalV1RootGroupScope(
    val kind: CanonicalV1RootGroupScopeKind,
    val groupSubjectId: String? = null,
) {
    init {
        require(
            when (kind) {
                CanonicalV1RootGroupScopeKind.GROUP ->
                    !groupSubjectId.isNullOrBlank()
                CanonicalV1RootGroupScopeKind.NO_GROUP ->
                    groupSubjectId == null
            },
        ) {
            "GROUP root scope requires a canonical Group subject id; NoGroup must not carry one"
        }
    }

    companion object {
        fun group(groupSubjectId: String): CanonicalV1RootGroupScope =
            CanonicalV1RootGroupScope(
                kind = CanonicalV1RootGroupScopeKind.GROUP,
                groupSubjectId = groupSubjectId,
            )

        val NoGroup =
            CanonicalV1RootGroupScope(
                kind = CanonicalV1RootGroupScopeKind.NO_GROUP,
            )
    }
}

enum class CanonicalV1PrimaryEvidence {
    NONE,
    CANONICAL_ROOT,
    CANONICAL_PARENT,
}

enum class CanonicalV1HierarchySourceAuthority {
    MAIN_BEACON_ROOT,
    MAIN_BEACON_GROUP_ROOT_PROJECTION,
    MAIN_BEACON_NO_GROUP_ROOT_PROJECTION,
    MAIN_BEACON_PARENT,
    MAIN_BEACON_PARENT_LINK,
    BEACON_OPERATIONAL_OWNER_PROJECTION,
    WORKSPACE_ROOT,
    WORKSPACE_ROOT_PROJECTION,
    WORKSPACE_PARENT,
    CONTEXT_PARENT_LINK,
}

enum class CanonicalV1HierarchyDiagnosticCode {
    AMBIGUOUS_PRIMARY_EVIDENCE,
}

data class CanonicalV1HierarchyDiagnostic(
    val code: CanonicalV1HierarchyDiagnosticCode,
    val target: HierarchyTargetRef,
    val occurrenceKeys: List<String>,
    val message: String,
)

data class CanonicalV1WorkspaceSnapshotInput(
    val id: String,
    val name: String,
    val parentWorkspaceId: String?,
    val order: Long,
    val sourceOrdinal: Int = 0,
)

data class CanonicalV1BeaconSnapshotInput(
    val legacyBeaconId: String,
    val target: HierarchyTargetRef,
    val title: String,
    val order: Long,
    val parentBeaconId: String?,
    val relatedOwnerIds: List<String>,
    val groupIds: List<String>,
    val groupOrders: Map<String, Long> = emptyMap(),
    val sourceOrdinal: Int = 0,
)

data class CanonicalV1BeaconGroupSnapshotInput(
    val id: String,
    val title: String,
    val order: Long,
    val canonicalSubjectId: String,
    val sourceOrdinal: Int = 0,
) {
    init {
        require(canonicalSubjectId.isNotBlank()) {
            "Beacon Group snapshot input requires canonicalSubjectId"
        }
    }
}

data class CanonicalV1ContextParentLinkSnapshotInput(
    val parentWorkspaceId: String,
    val childWorkspaceId: String,
    val order: Long,
    val sourceOrdinal: Int = 0,
)

data class CanonicalV1BeaconParentLinkSnapshotInput(
    val parentBeaconId: String,
    val childBeaconId: String,
    val order: Long,
    val sourceOrdinal: Int = 0,
)

data class CanonicalV1HierarchySnapshotInput(
    val workspaces: List<CanonicalV1WorkspaceSnapshotInput>,
    val beacons: List<CanonicalV1BeaconSnapshotInput>,
    val groups: List<CanonicalV1BeaconGroupSnapshotInput> = emptyList(),
    val contextParentLinks: List<CanonicalV1ContextParentLinkSnapshotInput> = emptyList(),
    val beaconParentLinks: List<CanonicalV1BeaconParentLinkSnapshotInput> = emptyList(),
)
