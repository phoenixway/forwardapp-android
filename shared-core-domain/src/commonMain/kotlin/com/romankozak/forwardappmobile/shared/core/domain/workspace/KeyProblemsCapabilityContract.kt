@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblem
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemAttachmentRef
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemWorkspaceRef
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import kotlin.js.JsExport
import kotlinx.serialization.json.*

/** KEY_PROBLEMS configuration v1 intentionally owns no configurable fields. */
data object KeyProblemsCapabilityConfigurationV1

object KeyProblemsCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1

    override val currentVersion: Int = CURRENT_VERSION

    override fun encodeDefault(): String = "{}"

    override fun validate(
        version: Int,
        raw: String,
    ) {
        require(version == CURRENT_VERSION) {
            "Unsupported KEY_PROBLEMS configuration version: $version"
        }
        require(raw.trim() == "{}") {
            "Invalid KEY_PROBLEMS configuration v1"
        }
    }

    fun encode(
        configuration: KeyProblemsCapabilityConfigurationV1 =
            KeyProblemsCapabilityConfigurationV1,
    ): String = encodeDefault()

    fun decode(
        version: Int,
        raw: String,
    ): KeyProblemsCapabilityConfigurationV1 {
        validate(version, raw)
        return KeyProblemsCapabilityConfigurationV1
    }
}

data class KeyProblemsContractViolation(
    val path: String,
    val code: String,
    val message: String,
)

fun validateKeyProblemsContract(
    problems: List<WorkspaceProblem>,
    workspaceRefs: List<WorkspaceProblemWorkspaceRef>,
    attachmentRefs: List<WorkspaceProblemAttachmentRef>,
): List<KeyProblemsContractViolation> {
    val violations = mutableListOf<KeyProblemsContractViolation>()
    val problemById = problems.associateBy { it.id }
    val liveProblems = problems.filterNot { it.isDeleted }

    addDuplicateIdViolations(problems.map { it.id }, "problems", violations)
    addDuplicateIdViolations(workspaceRefs.map { it.id }, "workspaceRefs", violations)
    addDuplicateIdViolations(attachmentRefs.map { it.id }, "attachmentRefs", violations)

    liveProblems.groupBy { it.capabilityInstanceId }
        .forEach { (capabilityId, owned) ->
            owned.groupBy { it.order }
                .filterValues { it.size > 1 }
                .forEach { (order, _) ->
                    violations +=
                        KeyProblemsContractViolation(
                            path = "problems.$capabilityId.$order",
                            code = "DUPLICATE_ORDER",
                            message = "Live problems must have unique order within a capability instance",
                        )
                }
        }

    liveProblems.filter { it.workspaceId.isBlank() || it.capabilityInstanceId.isBlank() }
        .forEach { problem ->
            violations +=
                KeyProblemsContractViolation(
                    path = "problems.${problem.id}.owner",
                    code = "INVALID_OWNER",
                    message = "Problem requires Workspace and capability-instance ownership",
                )
        }

    liveProblems.filter { it.order < 0L }.forEach { problem ->
        violations +=
            KeyProblemsContractViolation(
                path = "problems.${problem.id}.order",
                code = "NEGATIVE_ORDER",
                message = "Problem order must not be negative",
            )
    }

    validateWorkspaceRefs(workspaceRefs, problemById, violations)
    validateAttachmentRefs(attachmentRefs, problemById, violations)
    validateProblemContent(liveProblems, workspaceRefs, attachmentRefs, violations)
    return violations
}

/** Kotlin/JS transport adapter; canonical relation rules remain in [validateKeyProblemsContract]. */
@JsExport
fun validateKeyProblemsContractWire(rawGraph: String): Array<String> {
    val root = Json.parseToJsonElement(rawGraph).jsonObject
    return validateKeyProblemsContract(
        problems = root.rows("workspaceProblems").map { row ->
            WorkspaceProblem(
                id = row.string("id"),
                createdAt = row.long("createdAt"),
                updatedAt = row.long("updatedAt"),
                syncedAt = row.nullableLong("syncedAt"),
                isDeleted = row.boolean("isDeleted"),
                version = row.long("version"),
                workspaceId = row.string("workspaceId"),
                capabilityInstanceId = row.string("capabilityInstanceId"),
                title = row.string("title"),
                description = row.string("description"),
                status = WorkspaceProblemStatus.valueOf(row.string("status")),
                order = row.long("order"),
            )
        },
        workspaceRefs = root.rows("workspaceProblemWorkspaceRefs").map { row ->
            WorkspaceProblemWorkspaceRef(
                id = row.string("id"),
                createdAt = row.long("createdAt"),
                updatedAt = row.long("updatedAt"),
                syncedAt = row.nullableLong("syncedAt"),
                isDeleted = row.boolean("isDeleted"),
                version = row.long("version"),
                problemId = row.string("problemId"),
                targetWorkspaceId = row.string("targetWorkspaceId"),
            )
        },
        attachmentRefs = root.rows("workspaceProblemAttachmentRefs").map { row ->
            WorkspaceProblemAttachmentRef(
                id = row.string("id"),
                createdAt = row.long("createdAt"),
                updatedAt = row.long("updatedAt"),
                syncedAt = row.nullableLong("syncedAt"),
                isDeleted = row.boolean("isDeleted"),
                version = row.long("version"),
                problemId = row.string("problemId"),
                attachmentId = row.string("attachmentId"),
            )
        },
    ).map { violation -> "${violation.code}:${violation.message}" }.toTypedArray()
}

/** Kotlin/JS adapter for the existing typed capability configuration codec. */
@JsExport
fun validateKeyProblemsCapabilityConfigurationWire(
    version: Int,
    raw: String,
): Array<String> =
    try {
        KeyProblemsCapabilityConfigurationCodec.validate(version, raw)
        emptyArray()
    } catch (error: IllegalArgumentException) {
        arrayOf(error.message ?: "Invalid KEY_PROBLEMS configuration")
    }

private fun addDuplicateIdViolations(
    ids: List<String>,
    path: String,
    violations: MutableList<KeyProblemsContractViolation>,
) {
    ids.groupingBy { it }.eachCount().filterValues { it > 1 }.forEach { (id, _) ->
        violations +=
            KeyProblemsContractViolation(
                path = "$path.$id",
                code = "DUPLICATE_ID",
                message = "Canonical identity must be unique",
            )
    }
}

private fun validateWorkspaceRefs(
    refs: List<WorkspaceProblemWorkspaceRef>,
    problemById: Map<String, WorkspaceProblem>,
    violations: MutableList<KeyProblemsContractViolation>,
) {
    refs.filterNot { it.isDeleted }.forEach { ref ->
        if (ref.targetWorkspaceId.isBlank()) {
            violations += violation("workspaceRefs.${ref.id}.target", "BLANK_TARGET", "Workspace target is blank")
        }
        if (problemById[ref.problemId]?.isDeleted != false) {
            violations += violation("workspaceRefs.${ref.id}.problem", "MISSING_PROBLEM", "Live ref requires a live problem")
        }
    }
    refs.filterNot { it.isDeleted }
        .groupBy { it.problemId to it.targetWorkspaceId }
        .filterValues { it.size > 1 }
        .forEach { (key, _) ->
            violations += violation("workspaceRefs.$key", "DUPLICATE_TARGET", "Workspace refs are an unordered set")
        }
}

private fun validateAttachmentRefs(
    refs: List<WorkspaceProblemAttachmentRef>,
    problemById: Map<String, WorkspaceProblem>,
    violations: MutableList<KeyProblemsContractViolation>,
) {
    refs.filterNot { it.isDeleted }.forEach { ref ->
        if (ref.attachmentId.isBlank()) {
            violations += violation("attachmentRefs.${ref.id}.target", "BLANK_TARGET", "Attachment target is blank")
        }
        if (problemById[ref.problemId]?.isDeleted != false) {
            violations += violation("attachmentRefs.${ref.id}.problem", "MISSING_PROBLEM", "Live ref requires a live problem")
        }
    }
    refs.filterNot { it.isDeleted }
        .groupBy { it.problemId to it.attachmentId }
        .filterValues { it.size > 1 }
        .forEach { (key, _) ->
            violations += violation("attachmentRefs.$key", "DUPLICATE_TARGET", "Attachment refs are an unordered set")
        }
}

private fun validateProblemContent(
    problems: List<WorkspaceProblem>,
    workspaceRefs: List<WorkspaceProblemWorkspaceRef>,
    attachmentRefs: List<WorkspaceProblemAttachmentRef>,
    violations: MutableList<KeyProblemsContractViolation>,
) {
    val linkedProblemIds =
        workspaceRefs.filterNot { it.isDeleted }.map { it.problemId }.toSet() +
            attachmentRefs.filterNot { it.isDeleted }.map { it.problemId }.toSet()
    problems.filter {
        it.title.isBlank() && it.description.isBlank() && it.id !in linkedProblemIds
    }.forEach { problem ->
        violations += violation("problems.${problem.id}", "EMPTY_PROBLEM", "Problem has no content or relations")
    }
}

private fun violation(
    path: String,
    code: String,
    message: String,
) = KeyProblemsContractViolation(path, code, message)

private fun JsonObject.rows(name: String): List<JsonObject> =
    (get(name) as? JsonArray)?.map { it.jsonObject } ?: emptyList()

private fun JsonObject.string(name: String): String = getValue(name).jsonPrimitive.content

private fun JsonObject.long(name: String): Long = getValue(name).jsonPrimitive.long

private fun JsonObject.nullableLong(name: String): Long? = get(name)?.jsonPrimitive?.longOrNull

private fun JsonObject.boolean(name: String): Boolean = getValue(name).jsonPrimitive.boolean
