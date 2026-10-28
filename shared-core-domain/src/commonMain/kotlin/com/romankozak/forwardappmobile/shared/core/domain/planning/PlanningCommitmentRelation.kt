package com.romankozak.forwardappmobile.shared.core.domain.planning

data class PlanningCommitmentRelationId(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class PlanningCommitmentRelationType { COMPONENT_OF }

/** Participation composition only. Neither a semantic relation nor a scope hierarchy. */
data class PlanningCommitmentRelation(
    val id: PlanningCommitmentRelationId,
    val sourceCommitmentId: PlanningCommitmentId,
    val targetCommitmentId: PlanningCommitmentId,
    val relationType: PlanningCommitmentRelationType,
    val order: Long,
    val createdAt: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

/** Linear graph validation, independent of horizon, role and Orientation kind. */
fun validatePlanningCommitmentRelations(
    relations: Collection<PlanningCommitmentRelation>,
    liveCommitmentIds: Set<PlanningCommitmentId>,
): List<String> = buildList {
    if (relations.map { it.id }.distinct().size != relations.size) add("Duplicate relation identity")
    relations.forEach {
        if (it.sourceCommitmentId == it.targetCommitmentId) add("Self relation: ${it.id.value}")
        if (it.version < 1 || it.createdAt < 0 || it.updatedAt?.let { t -> t < it.createdAt } == true ||
            it.syncedAt?.let { t -> t < 0 } == true) add("Invalid relation revision: ${it.id.value}")
    }
    val live = relations.filterNot { it.isDeleted }
    if (live.map { Triple(it.sourceCommitmentId, it.targetCommitmentId, it.relationType) }.distinct().size != live.size) {
        add("Duplicate live relation tuple")
    }
    live.forEach {
        if (it.sourceCommitmentId !in liveCommitmentIds || it.targetCommitmentId !in liveCommitmentIds) {
            add("Unavailable commitment endpoint: ${it.id.value}")
        }
    }
    val graph = live.groupBy { it.sourceCommitmentId }
    val degrees = mutableMapOf<PlanningCommitmentId, Int>()
    live.forEach {
        if (it.sourceCommitmentId !in degrees) degrees[it.sourceCommitmentId] = 0
        degrees[it.targetCommitmentId] = (degrees[it.targetCommitmentId] ?: 0) + 1
    }
    val queue = ArrayDeque(degrees.filterValues { it == 0 }.keys)
    var visited = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        visited++
        graph[node].orEmpty().forEach {
            val remaining = degrees.getValue(it.targetCommitmentId) - 1
            degrees[it.targetCommitmentId] = remaining
            if (remaining == 0) queue.addLast(it.targetCommitmentId)
        }
    }
    if (visited != degrees.size) add("COMPONENT_OF must be acyclic")
}

/** Explicit carry mapping. Only outgoing edges of selected sources are recreated. */
fun remapPlanningComposition(
    relations: Collection<PlanningCommitmentRelation>,
    replacements: Map<PlanningCommitmentId, PlanningCommitmentId>,
    liveCommitmentIds: Set<PlanningCommitmentId>,
): List<Pair<PlanningCommitmentId, PlanningCommitmentId>> {
    require(replacements.values.distinct().size == replacements.size) { "Ambiguous carry mapping" }
    require(replacements.values.all { it in liveCommitmentIds }) { "Missing carried endpoint" }
    return relations.filter { !it.isDeleted && it.sourceCommitmentId in replacements }
        .sortedWith(compareBy<PlanningCommitmentRelation> { it.order }.thenBy { it.id.value })
        .map {
            val source = replacements.getValue(it.sourceCommitmentId)
            val target = replacements[it.targetCommitmentId] ?: it.targetCommitmentId
            require(target in liveCommitmentIds) { "Missing carry target" }
            source to target
        }.distinct()
}
