package com.romankozak.forwardappmobile.shared.core.domain.orientation

import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityArchetype
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityAvailability
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityDefinition
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityInstance
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType

val orientationCapabilityRegistry: List<WorkspaceCapabilityDefinition> =
    WorkspaceCapabilityType.entries.map { type ->
        val legacyIds =
            when (type) {
                WorkspaceCapabilityType.BACKLOG -> listOf("backlog")
                WorkspaceCapabilityType.INBOX -> listOf("inbox")
                WorkspaceCapabilityType.INBOX_SORTING -> listOf("inbox_sorting")
                WorkspaceCapabilityType.KEY_PROBLEMS -> listOf("key_problems")
                WorkspaceCapabilityType.DIRECTION -> listOf("direction")
                WorkspaceCapabilityType.ARTIFACT -> listOf("artifact")
                WorkspaceCapabilityType.DASHBOARD -> listOf("dashboard")
                WorkspaceCapabilityType.JOURNAL -> listOf("journal_log")
                WorkspaceCapabilityType.EXECUTION_LOG -> listOf("log")
                WorkspaceCapabilityType.CONNECTIONS -> listOf("connections", "attachments")
                WorkspaceCapabilityType.DOCUMENTS,
                WorkspaceCapabilityType.NOTES,
                WorkspaceCapabilityType.ATTACHMENTS,
                -> emptyList()
            }
        WorkspaceCapabilityDefinition(
            type = type,
            archetype = capabilityArchetype(type),
            availability = capabilityAvailability(type),
            maxActiveInstances = 1,
            requiredTypes = emptyList(),
            legacyIds = legacyIds,
            autoMigrateWhenEnabled = legacyIds.isNotEmpty(),
        )
    }

private fun capabilityArchetype(type: WorkspaceCapabilityType): WorkspaceCapabilityArchetype =
    when (type) {
        WorkspaceCapabilityType.DASHBOARD -> WorkspaceCapabilityArchetype.PRESENTATION
        WorkspaceCapabilityType.INBOX,
        WorkspaceCapabilityType.KEY_PROBLEMS,
        WorkspaceCapabilityType.EXECUTION_LOG,
        -> WorkspaceCapabilityArchetype.OWNED_COLLECTION
        WorkspaceCapabilityType.BACKLOG,
        WorkspaceCapabilityType.DIRECTION,
        WorkspaceCapabilityType.CONNECTIONS,
        -> WorkspaceCapabilityArchetype.ORDERED_PLACEMENT
        WorkspaceCapabilityType.INBOX_SORTING -> WorkspaceCapabilityArchetype.POLICY
        WorkspaceCapabilityType.DOCUMENTS,
        WorkspaceCapabilityType.NOTES,
        WorkspaceCapabilityType.ATTACHMENTS,
        -> WorkspaceCapabilityArchetype.CONTENT_HOST
        WorkspaceCapabilityType.ARTIFACT,
        WorkspaceCapabilityType.JOURNAL,
        -> WorkspaceCapabilityArchetype.RETIRED_LEGACY
    }

private fun capabilityAvailability(type: WorkspaceCapabilityType): WorkspaceCapabilityAvailability =
    when (type) {
        WorkspaceCapabilityType.ARTIFACT,
        WorkspaceCapabilityType.JOURNAL,
        -> WorkspaceCapabilityAvailability.RETIRED
        WorkspaceCapabilityType.DOCUMENTS,
        WorkspaceCapabilityType.NOTES,
        WorkspaceCapabilityType.ATTACHMENTS,
        -> WorkspaceCapabilityAvailability.RESERVED
        else -> WorkspaceCapabilityAvailability.TARGET
    }

fun validateWorkspaceBindings(bindings: List<WorkspaceBinding>): List<OrientationContractViolation> {
    val live = bindings.filterNot { it.isDeleted }
    val violations = mutableListOf<OrientationContractViolation>()

    live.filter { it.isPrimary && it.bindingType != WorkspaceBindingType.EMBODIES }
        .forEach { binding ->
            violations +=
                OrientationContractViolation(
                    "workspaceBindings.${binding.id}",
                    "INVALID_PRIMARY",
                    "Only EMBODIES may be primary",
                )
        }

    live.filter { it.bindingType == WorkspaceBindingType.EMBODIES }
        .groupBy { it.subjectId }
        .filterValues { it.size > 1 }
        .forEach { (subjectId, _) ->
            violations +=
                OrientationContractViolation(
                    "workspaceBindings.$subjectId",
                    "MULTIPLE_EMBODIED_WORKSPACES",
                    "A subject may have at most one embodied Workspace",
                )
        }

    live.filter { it.bindingType == WorkspaceBindingType.EMBODIES }
        .groupBy { it.workspaceId }
        .filterValues { it.size > 1 }
        .forEach { (workspaceId, _) ->
            violations +=
                OrientationContractViolation(
                    "workspaceBindings.$workspaceId",
                    "MULTIPLE_EMBODIED_SUBJECTS",
                    "A Workspace may embody at most one subject",
                )
        }

    return violations
}

fun validateCapabilityInstances(instances: List<WorkspaceCapabilityInstance>): List<OrientationContractViolation> {
    val live = instances.filterNot { it.isDeleted }
    val definitions = orientationCapabilityRegistry.associateBy { it.type }
    val violations = mutableListOf<OrientationContractViolation>()

    live.filter { it.instanceKey.isBlank() }.forEach { instance ->
        violations +=
            OrientationContractViolation(
                "capabilities.${instance.id}.instanceKey",
                "BLANK_INSTANCE_KEY",
                "Capability instanceKey must not be blank",
            )
    }

    live.groupBy { Triple(it.workspaceId, it.capabilityType, it.instanceKey) }
        .filterValues { it.size > 1 }
        .forEach { (key, _) ->
            violations +=
                OrientationContractViolation(
                    "capabilities.$key",
                    "DUPLICATE_LOGICAL_ID",
                    "Capability logical identity must be unique",
                )
        }

    live.filter { it.state == WorkspaceCapabilityState.ACTIVE }
        .groupBy { it.workspaceId to it.capabilityType }
        .forEach { (key, active) ->
            val max = definitions.getValue(key.second).maxActiveInstances
            if (active.size > max) {
                violations +=
                    OrientationContractViolation(
                        "capabilities.$key",
                        "TOO_MANY_ACTIVE",
                        "At most $max active instance is allowed",
                    )
            }
        }

    val activeByWorkspace =
        live.filter { it.state == WorkspaceCapabilityState.ACTIVE }
            .groupBy { it.workspaceId }
            .mapValues { (_, values) -> values.map { it.capabilityType }.toSet() }
    live.filter { it.state == WorkspaceCapabilityState.ACTIVE }.forEach { instance ->
        definitions.getValue(instance.capabilityType).requiredTypes.forEach { required ->
            if (required !in activeByWorkspace[instance.workspaceId].orEmpty()) {
                violations +=
                    OrientationContractViolation(
                        "capabilities.${instance.id}",
                        "MISSING_DEPENDENCY",
                        "${instance.capabilityType} requires $required",
                    )
            }
        }
    }

    return violations
}
