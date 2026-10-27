package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.shared.core.domain.orientation.EmptyOrientationFilterEnvironment
import com.romankozak.forwardappmobile.shared.core.domain.orientation.OrientationFilterEnvironment
import com.romankozak.forwardappmobile.shared.core.domain.orientation.matchesOrientationFilter
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Preserve other environment authorities; only tag membership is supplied here. */
class SemanticTagFilterEnvironment(
    private val memberships: Map<ManagedSubjectRef, Set<String>>,
    private val delegate: OrientationFilterEnvironment = EmptyOrientationFilterEnvironment,
) : OrientationFilterEnvironment by delegate {
    override fun tags(orientationId: String) = memberships[ManagedSubjectRef(ManagedSubjectType.ORIENTATION, orientationId)].orEmpty()
}

fun CanonicalSemanticTagRepository.observeFilterEnvironment(
    delegate: OrientationFilterEnvironment = EmptyOrientationFilterEnvironment,
): Flow<OrientationFilterEnvironment> = observeAllTags().map { SemanticTagFilterEnvironment(it, delegate) }

/** Canonical Room-backed entry for the existing shared AST; evaluator semantics remain unchanged. */
fun CanonicalSemanticTagRepository.observeMatchingOrientations(
    items: Flow<List<EffectiveOrientation>>,
    filter: OrientationFilter,
    delegate: OrientationFilterEnvironment = EmptyOrientationFilterEnvironment,
): Flow<List<EffectiveOrientation>> = combine(items, observeFilterEnvironment(delegate)) { current, environment ->
    current.filter { matchesOrientationFilter(it, filter, environment) }
}
