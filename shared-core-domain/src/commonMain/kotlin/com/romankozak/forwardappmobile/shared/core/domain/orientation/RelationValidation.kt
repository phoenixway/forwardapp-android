package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelation
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType

private val acyclicRelationTypes =
    setOf(
        OrientationRelationType.PART_OF,
        OrientationRelationType.DEPENDS_ON,
        OrientationRelationType.PRECEDES,
        OrientationRelationType.REFINES,
        OrientationRelationType.DERIVED_FROM,
    )

fun validateOrientationRelations(
    orientationIds: Set<String>,
    relations: List<OrientationRelation>,
): List<OrientationContractViolation> {
    val live = relations.filterNot { it.isDeleted }
    val violations = mutableListOf<OrientationContractViolation>()

    live.forEach { relation ->
        val path = "relations.${relation.id}"
        if (relation.fromOrientationId !in orientationIds || relation.toOrientationId !in orientationIds) {
            violations += OrientationContractViolation(path, "UNKNOWN_ENDPOINT", "Both endpoints must exist")
        }
        if (relation.fromOrientationId == relation.toOrientationId) {
            violations += OrientationContractViolation(path, "SELF_EDGE", "Self-relations are forbidden")
        }
        if (relation.relationType == OrientationRelationType.PART_OF && relation.order == null) {
            violations += OrientationContractViolation(path, "MISSING_ORDER", "PART_OF requires sibling order")
        }
        if (
            relation.relationType == OrientationRelationType.CONFLICTS_WITH &&
            relation.fromOrientationId >= relation.toOrientationId
        ) {
            violations +=
                OrientationContractViolation(
                    path,
                    "NON_CANONICAL_PAIR",
                    "CONFLICTS_WITH endpoints must use lexical canonical order",
                )
        }
    }

    acyclicRelationTypes.forEach { type ->
        if (containsCycle(live.filter { it.relationType == type })) {
            violations += OrientationContractViolation("relations", "CYCLE", "$type graph must be acyclic")
        }
    }

    return violations
}

fun validateAspectOrientationRefs(refs: List<AspectOrientationRef>): List<OrientationContractViolation> {
    val live = refs.filterNot { it.isDeleted }
    val violations = mutableListOf<OrientationContractViolation>()

    live.filter { it.isPrimary && it.relationType != AspectOrientationRelationType.BELONGS_TO }
        .forEach { ref ->
            violations +=
                OrientationContractViolation(
                    "aspectRefs.${ref.id}",
                    "INVALID_PRIMARY",
                    "Only BELONGS_TO may be primary",
                )
        }

    live.filter { it.isPrimary && it.relationType == AspectOrientationRelationType.BELONGS_TO }
        .groupBy { it.orientationId }
        .filterValues { it.size > 1 }
        .forEach { (orientationId, _) ->
            violations +=
                OrientationContractViolation(
                    "aspectRefs.$orientationId",
                    "MULTIPLE_PRIMARY_ASPECTS",
                    "An Orientation may have at most one primary Aspect",
                )
        }

    return violations
}

fun validateSingleParentHierarchy(parentById: Map<String, String?>): List<OrientationContractViolation> {
    val violations = mutableListOf<OrientationContractViolation>()
    parentById.forEach { (id, parentId) ->
        if (parentId != null && parentId !in parentById) {
            violations += OrientationContractViolation("hierarchy.$id", "UNKNOWN_PARENT", "Parent $parentId does not exist")
        }
        if (parentId == id) {
            violations += OrientationContractViolation("hierarchy.$id", "SELF_PARENT", "An item cannot parent itself")
        }
    }

    parentById.keys.forEach { start ->
        val visited = mutableSetOf<String>()
        var current: String? = start
        while (current != null && current in parentById) {
            if (!visited.add(current)) {
                violations += OrientationContractViolation("hierarchy.$start", "CYCLE", "Hierarchy must be acyclic")
                break
            }
            current = parentById[current]
        }
    }
    return violations.distinctBy { it.path to it.code }
}

private fun containsCycle(relations: List<OrientationRelation>): Boolean {
    val outgoing = relations.groupBy { it.fromOrientationId }
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()

    fun visit(id: String): Boolean {
        if (id in visiting) return true
        if (!visited.add(id)) return false
        visiting += id
        val cycle = outgoing[id].orEmpty().any { visit(it.toOrientationId) }
        visiting -= id
        return cycle
    }

    return relations.any { visit(it.fromOrientationId) }
}
