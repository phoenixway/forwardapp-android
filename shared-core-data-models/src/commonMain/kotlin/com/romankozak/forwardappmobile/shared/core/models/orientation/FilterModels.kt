@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.models.orientation

import kotlin.js.JsExport

@JsExport
enum class FilterComparison {
    EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    GREATER_THAN,
    GREATER_OR_EQUAL,
}

@JsExport
enum class RelationDirection {
    OUTGOING,
    INCOMING,
    EITHER,
}

@JsExport
sealed interface OrientationFilter

@JsExport
data class AllFilter(val children: List<OrientationFilter>) : OrientationFilter

@JsExport
data class AnyFilter(val children: List<OrientationFilter>) : OrientationFilter

@JsExport
data class NotFilter(val child: OrientationFilter) : OrientationFilter

@JsExport
data class KindFilter(val kinds: List<OrientationKind>) : OrientationFilter

@JsExport
data class LifecycleFilter(val lifecycles: List<OrientationLifecycle>) : OrientationFilter

@JsExport
data class AxisCompareFilter(
    val axis: OrientationAxis,
    val comparison: FilterComparison,
    val valueCode: String,
) : OrientationFilter

@JsExport
data class AxisOriginFilter(
    val axis: OrientationAxis,
    val origins: List<ValueOrigin>,
) : OrientationFilter

@JsExport
data class AspectMembershipFilter(
    val aspectId: String,
    val includeDescendants: Boolean,
) : OrientationFilter

@JsExport
data class RelationFilter(
    val relationType: OrientationRelationType,
    val direction: RelationDirection,
    val targetOrientationId: String?,
    val maxDepth: Int,
) : OrientationFilter

@JsExport
data class WorkspaceCapabilityFilter(
    val capabilityType: WorkspaceCapabilityType,
) : OrientationFilter

@JsExport
data class PlanningCoverageFilter(val covered: Boolean) : OrientationFilter

@JsExport
data class ContributionFilter(
    val roles: List<ContributionRole>,
    val attributionMode: AttributionMode?,
) : OrientationFilter

@JsExport
data class TagFilter(val normalizedTags: List<String>) : OrientationFilter

@JsExport
data class TextFilter(val query: String) : OrientationFilter

@JsExport
enum class OrientationTimeField {
    CREATED_AT,
    UPDATED_AT,
    TARGET,
    ACTIVITY,
}

@JsExport
data class TimeWindowFilter(
    val field: OrientationTimeField,
    val fromInclusive: Long?,
    val toExclusive: Long?,
) : OrientationFilter

@JsExport
data class SavedOrientationView(
    val id: String,
    val title: String,
    val filterAstVersion: Int,
    val filter: OrientationFilter,
    val sortSpecification: String,
    val grouping: String?,
    val visibleFields: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val version: Long,
    val isDeleted: Boolean,
)

const val ORIENTATION_FILTER_AST_VERSION: Int = 1
const val ORIENTATION_FILTER_MAX_RELATION_DEPTH: Int = 8
