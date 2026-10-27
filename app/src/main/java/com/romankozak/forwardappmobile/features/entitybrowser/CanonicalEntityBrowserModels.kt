package com.romankozak.forwardappmobile.features.entitybrowser

import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind

enum class CanonicalEntityClass {
    ORIENTATION,
    ASPECT,
    WORKSPACE,
}

sealed interface CanonicalEntityRef {
    val id: String
    val entityClass: CanonicalEntityClass

    data class Orientation(override val id: String) : CanonicalEntityRef {
        override val entityClass = CanonicalEntityClass.ORIENTATION
    }

    data class Aspect(override val id: String) : CanonicalEntityRef {
        override val entityClass = CanonicalEntityClass.ASPECT
    }

    data class Workspace(override val id: String) : CanonicalEntityRef {
        override val entityClass = CanonicalEntityClass.WORKSPACE
    }
}

data class CanonicalEntitySummary(
    val ref: CanonicalEntityRef,
    val title: String,
    val subtitle: String? = null,
    val orientationKind: OrientationKind? = null,
    val isArchivedAspect: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val tags: Set<String> = emptySet(),
    val scores: Map<CanonicalScoreDimension, Double> = emptyMap(),
)

data class CanonicalEntityBrowserQuery(
    val text: String = "",
    val allowedClasses: Set<CanonicalEntityClass> = CanonicalEntityClass.entries.toSet(),
    val excludedRefs: Set<CanonicalEntityRef> = emptySet(),
    val includeArchivedAspects: Boolean = false,
)

fun queryCanonicalEntities(
    catalog: List<CanonicalEntitySummary>,
    query: CanonicalEntityBrowserQuery,
): List<CanonicalEntitySummary> {
    return filterCanonicalEntities(catalog, query)
        .sortedWith(compareBy<CanonicalEntitySummary>({ it.title.lowercase() }, { it.ref.entityClass.name }, { it.ref.id }))
}

/** Unsorted matching for callers that apply their own explicit ordering. */
fun filterCanonicalEntities(
    catalog: List<CanonicalEntitySummary>,
    query: CanonicalEntityBrowserQuery,
): List<CanonicalEntitySummary> {
    val needle = query.text.trim().lowercase()
    return catalog.asSequence()
        .filter { it.ref.entityClass in query.allowedClasses }
        .filterNot { it.ref in query.excludedRefs }
        .filter { query.includeArchivedAspects || !it.isArchivedAspect }
        .filter { canonicalTitleMatches(it.title, needle) }
        .toList()
}

fun CanonicalEntityClass.displayLabel(): String =
    when (this) {
        CanonicalEntityClass.ORIENTATION -> "Орієнтир"
        CanonicalEntityClass.ASPECT -> "Аспект"
        CanonicalEntityClass.WORKSPACE -> "Робочий простір"
    }
