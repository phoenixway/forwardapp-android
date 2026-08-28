package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AllFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.AnyFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectMembershipFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisCompareFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisOriginFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.ContributionFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.EffectiveOrientation
import com.romankozak.forwardappmobile.shared.core.models.orientation.FilterComparison
import com.romankozak.forwardappmobile.shared.core.models.orientation.KindFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.LifecycleFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.NotFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.ORIENTATION_FILTER_MAX_RELATION_DEPTH
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.PlanningCoverageFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.RelationFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.TagFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.TextFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.TimeWindowFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityFilter

interface OrientationFilterEnvironment {
    fun belongsToAspect(orientationId: String, aspectId: String, includeDescendants: Boolean): Boolean

    fun matchesRelation(orientationId: String, filter: RelationFilter): Boolean

    fun hasWorkspaceCapability(orientationId: String, filter: WorkspaceCapabilityFilter): Boolean

    fun hasPlanningCoverage(orientationId: String): Boolean

    fun matchesContribution(orientationId: String, filter: ContributionFilter): Boolean

    fun tags(orientationId: String): Set<String>

    fun matchesTimeWindow(orientationId: String, filter: TimeWindowFilter): Boolean
}

object EmptyOrientationFilterEnvironment : OrientationFilterEnvironment {
    override fun belongsToAspect(orientationId: String, aspectId: String, includeDescendants: Boolean) = false

    override fun matchesRelation(orientationId: String, filter: RelationFilter) = false

    override fun hasWorkspaceCapability(orientationId: String, filter: WorkspaceCapabilityFilter) = false

    override fun hasPlanningCoverage(orientationId: String) = false

    override fun matchesContribution(orientationId: String, filter: ContributionFilter) = false

    override fun tags(orientationId: String) = emptySet<String>()

    override fun matchesTimeWindow(orientationId: String, filter: TimeWindowFilter) = false
}

fun validateOrientationFilter(filter: OrientationFilter): List<OrientationContractViolation> =
    when (filter) {
        is AllFilter -> filter.children.flatMap(::validateOrientationFilter)
        is AnyFilter -> filter.children.flatMap(::validateOrientationFilter)
        is NotFilter -> validateOrientationFilter(filter.child)
        is AxisCompareFilter -> {
            val values = axisValueCodes(filter.axis)
            if (filter.valueCode in values) emptyList()
            else listOf(OrientationContractViolation("filter.axis", "UNKNOWN_VALUE", filter.valueCode))
        }

        is RelationFilter ->
            if (filter.maxDepth in 1..ORIENTATION_FILTER_MAX_RELATION_DEPTH) emptyList()
            else listOf(OrientationContractViolation("filter.maxDepth", "OUT_OF_RANGE", "Relation depth must be 1..8"))

        is TextFilter ->
            if (filter.query.isBlank()) {
                listOf(OrientationContractViolation("filter.query", "BLANK_QUERY", "Text query must not be blank"))
            } else {
                emptyList()
            }

        is TimeWindowFilter ->
            if (invalidTimeWindow(filter)) {
                listOf(OrientationContractViolation("filter.time", "INVALID_WINDOW", "from must be before to"))
            } else {
                emptyList()
            }

        is KindFilter,
        is LifecycleFilter,
        is AxisOriginFilter,
        is AspectMembershipFilter,
        is WorkspaceCapabilityFilter,
        is PlanningCoverageFilter,
        is ContributionFilter,
        is TagFilter,
        -> emptyList()
    }

private fun invalidTimeWindow(filter: TimeWindowFilter): Boolean {
    val from = filter.fromInclusive ?: return false
    val to = filter.toExclusive ?: return false
    return from >= to
}

fun matchesOrientationFilter(
    item: EffectiveOrientation,
    filter: OrientationFilter,
    environment: OrientationFilterEnvironment = EmptyOrientationFilterEnvironment,
): Boolean {
    require(validateOrientationFilter(filter).isEmpty()) { "Invalid Orientation Filter AST" }
    val orientationId = item.subject.id
    return when (filter) {
        is AllFilter -> filter.children.all { matchesOrientationFilter(item, it, environment) }
        is AnyFilter -> filter.children.any { matchesOrientationFilter(item, it, environment) }
        is NotFilter -> !matchesOrientationFilter(item, filter.child, environment)
        is KindFilter -> item.orientation.kind in filter.kinds
        is LifecycleFilter -> item.orientation.lifecycle in filter.lifecycles
        is AxisOriginFilter -> item.orientation.assessment.valueFor(filter.axis).origin in filter.origins
        is AxisCompareFilter -> compareAxis(item, filter)
        is AspectMembershipFilter -> environment.belongsToAspect(orientationId, filter.aspectId, filter.includeDescendants)
        is RelationFilter -> environment.matchesRelation(orientationId, filter)
        is WorkspaceCapabilityFilter -> environment.hasWorkspaceCapability(orientationId, filter)
        is PlanningCoverageFilter -> environment.hasPlanningCoverage(orientationId) == filter.covered
        is ContributionFilter -> environment.matchesContribution(orientationId, filter)
        is TagFilter -> environment.tags(orientationId).containsAll(filter.normalizedTags.map { it.lowercase() })
        is TextFilter -> matchesText(item, filter.query)
        is TimeWindowFilter -> environment.matchesTimeWindow(orientationId, filter)
    }
}

private fun compareAxis(
    item: EffectiveOrientation,
    filter: AxisCompareFilter,
): Boolean {
    val order = axisValueCodes(filter.axis)
    val actual = item.orientation.assessment.valueFor(filter.axis).valueCode ?: return false
    val actualIndex = order.indexOf(actual)
    val expectedIndex = order.indexOf(filter.valueCode)
    if (actualIndex < 0 || expectedIndex < 0) return false
    return when (filter.comparison) {
        FilterComparison.EQUAL -> actualIndex == expectedIndex
        FilterComparison.LESS_THAN -> actualIndex < expectedIndex
        FilterComparison.LESS_OR_EQUAL -> actualIndex <= expectedIndex
        FilterComparison.GREATER_THAN -> actualIndex > expectedIndex
        FilterComparison.GREATER_OR_EQUAL -> actualIndex >= expectedIndex
    }
}

private fun matchesText(
    item: EffectiveOrientation,
    query: String,
): Boolean {
    val normalized = query.trim().lowercase()
    return item.subject.title.lowercase().contains(normalized) ||
        item.subject.description?.lowercase()?.contains(normalized) == true
}
