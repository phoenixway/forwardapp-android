package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

object LegacyKeyProblemsPayloadParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(source: LegacyKeyProblemsSource): ParsedKeyProblemsSource {
        val root =
            runCatching { json.parseToJsonElement(source.payloadJson) }.getOrNull()
                as? JsonObject
                ?: return failure(
                    source,
                    KeyProblemsMigrationIssueCode.INVALID_JSON,
                    "Payload must be a valid JSON object",
                )

        val tracker = parseTracker(source, root)
        if (tracker != null && (tracker.problems.isNotEmpty() || tracker.issues.isNotEmpty())) {
            return tracker
        }
        return parseLegacy(source, root)
    }

    private fun parseTracker(
        source: LegacyKeyProblemsSource,
        root: JsonObject,
    ): ParsedKeyProblemsSource? {
        val rawIssues = root["issues"] ?: return null
        if (rawIssues !is JsonArray) {
            return failure(
                source,
                KeyProblemsMigrationIssueCode.INVALID_PAYLOAD_SHAPE,
                "issues must be an array",
            )
        }

        val parsed = mutableListOf<ParsedLegacyProblem>()
        val issues = mutableListOf<KeyProblemsMigrationIssue>()
        addRawDuplicateIssueDiagnostics(source, rawIssues, issues)
        rawIssues.forEachIndexed { index, element ->
            parseIssue(source, index, element, issues)?.let(parsed::add)
        }
        return ParsedKeyProblemsSource(source, parsed, issues)
    }

    private fun parseIssue(
        source: LegacyKeyProblemsSource,
        index: Int,
        element: JsonElement,
        issues: MutableList<KeyProblemsMigrationIssue>,
    ): ParsedLegacyProblem? {
        val item = element as? JsonObject
        if (item == null) {
            issues += source.issue(null, KeyProblemsMigrationIssueCode.INVALID_ISSUE, "Issue $index must be an object")
            return null
        }

        val id = item.requiredString("id")
        if (id == null) {
            issues += source.issue(null, KeyProblemsMigrationIssueCode.BLANK_ISSUE_ID, "Issue $index has no non-blank id")
            return null
        }

        val title = item.optionalString("title", "", source, id, issues) ?: return null
        val description = item.optionalString("description", "", source, id, issues) ?: return null
        val status = parseStatus(item, source, id, issues) ?: return null
        val contexts = item.optionalStringArray("relatedContextIds", source, id, issues) ?: return null
        val attachments = item.optionalStringArray("relatedAttachmentIds", source, id, issues) ?: return null
        val order = item.optionalLong("order", 0L, source, id, issues) ?: return null
        val createdAt = item.optionalLong("createdAt", source.updatedAt, source, id, issues) ?: return null
        val updatedAt = item.optionalLong("updatedAt", source.updatedAt, source, id, issues) ?: return null

        val dateTime = item["dateTime"]
        if (dateTime != null && dateTime !is JsonNull) {
            val value = (dateTime as? JsonPrimitive)?.longOrNull
            if (value == null) {
                issues += source.issue(id, KeyProblemsMigrationIssueCode.INVALID_ISSUE, "dateTime must be an integer or null")
            } else {
                issues +=
                    source.issue(
                        id,
                        KeyProblemsMigrationIssueCode.DATE_TIME_REQUIRES_DECISION,
                        "Legacy dateTime=$value has no accepted canonical meaning",
                    )
            }
        }

        if (title.trim().isEmpty() && description.trim().isEmpty() && contexts.isEmpty() && attachments.isEmpty()) {
            issues += source.issue(id, KeyProblemsMigrationIssueCode.EMPTY_ISSUE, "Issue has no content or relations")
        }

        return ParsedLegacyProblem(
            id = id,
            title = title.trim(),
            description = description.trim(),
            status = status,
            relatedContextIds = contexts.distinct(),
            relatedAttachmentIds = attachments.distinct(),
            sourceOrder = order,
            sourceIndex = index,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun parseLegacy(
        source: LegacyKeyProblemsSource,
        root: JsonObject,
    ): ParsedKeyProblemsSource {
        val issues = mutableListOf<KeyProblemsMigrationIssue>()
        val description = root.optionalString("description", "", source, null, issues)
        val contextIds = root.optionalStringArray("focusContextIds", source, null, issues)
        if (description == null || contextIds == null || issues.isNotEmpty()) {
            return ParsedKeyProblemsSource(source, emptyList(), issues)
        }

        val normalizedDescription = description.trim()
        val normalizedContexts = contextIds.distinct()
        if (normalizedDescription.isEmpty() && normalizedContexts.isEmpty()) {
            return ParsedKeyProblemsSource(source, emptyList(), emptyList())
        }

        val title =
            normalizedDescription.lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.take(80)
                ?.trim()
                .orEmpty()
                .ifBlank { "Imported issue" }
        return ParsedKeyProblemsSource(
            source = source,
            problems =
                listOf(
                    ParsedLegacyProblem(
                        id = "legacy-${source.contextId}",
                        title = title,
                        description = normalizedDescription,
                        status = WorkspaceProblemStatus.OPEN,
                        relatedContextIds = normalizedContexts,
                        relatedAttachmentIds = emptyList(),
                        sourceOrder = 0L,
                        sourceIndex = 0,
                        createdAt = source.updatedAt,
                        updatedAt = source.updatedAt,
                    ),
                ),
            issues = emptyList(),
        )
    }

    private fun parseStatus(
        item: JsonObject,
        source: LegacyKeyProblemsSource,
        id: String,
        issues: MutableList<KeyProblemsMigrationIssue>,
    ): WorkspaceProblemStatus? {
        val raw = item.optionalString("status", WorkspaceProblemStatus.OPEN.name, source, id, issues) ?: return null
        return WorkspaceProblemStatus.entries.firstOrNull { it.name == raw }.also { status ->
            if (status == null) {
                issues += source.issue(id, KeyProblemsMigrationIssueCode.UNKNOWN_STATUS, "Unknown issue status: $raw")
            }
        }
    }

    private fun addRawDuplicateIssueDiagnostics(
        source: LegacyKeyProblemsSource,
        rawIssues: JsonArray,
        issues: MutableList<KeyProblemsMigrationIssue>,
    ) {
        rawIssues.mapNotNull { (it as? JsonObject)?.requiredString("id") }
            .groupBy { it }
            .filterValues { it.size > 1 }
            .forEach { (id, _) ->
                issues +=
                    source.issue(
                        id,
                        KeyProblemsMigrationIssueCode.DUPLICATE_ISSUE_ID,
                        "Issue id is duplicated in payload",
                    )
            }
    }

    private fun failure(
        source: LegacyKeyProblemsSource,
        code: KeyProblemsMigrationIssueCode,
        detail: String,
    ) = ParsedKeyProblemsSource(source, emptyList(), listOf(source.issue(null, code, detail)))
}

private fun JsonObject.requiredString(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

private fun JsonObject.optionalString(
    key: String,
    default: String,
    source: LegacyKeyProblemsSource,
    issueId: String?,
    issues: MutableList<KeyProblemsMigrationIssue>,
): String? {
    val element = this[key] ?: return default
    val value = (element as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    if (value == null) {
        issues += source.issue(issueId, KeyProblemsMigrationIssueCode.INVALID_ISSUE, "$key must be a string")
    }
    return value
}

private fun JsonObject.optionalStringArray(
    key: String,
    source: LegacyKeyProblemsSource,
    issueId: String?,
    issues: MutableList<KeyProblemsMigrationIssue>,
): List<String>? {
    val element = this[key] ?: return emptyList()
    val array = element as? JsonArray
    val values = array?.mapNotNull { element ->
        (element as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }
    if (array == null || values == null || values.size != array.size) {
        issues += source.issue(issueId, KeyProblemsMigrationIssueCode.INVALID_ISSUE, "$key must contain only strings")
        return null
    }
    val normalized = values.map(String::trim)
    if (normalized.any(String::isEmpty)) {
        issues += source.issue(issueId, KeyProblemsMigrationIssueCode.INVALID_ISSUE, "$key must not contain blank ids")
        return null
    }
    return normalized
}

private fun JsonObject.optionalLong(
    key: String,
    default: Long,
    source: LegacyKeyProblemsSource,
    issueId: String,
    issues: MutableList<KeyProblemsMigrationIssue>,
): Long? {
    val element = this[key] ?: return default
    val value = (element as? JsonPrimitive)?.longOrNull
    if (value == null) {
        issues += source.issue(issueId, KeyProblemsMigrationIssueCode.INVALID_ISSUE, "$key must be an integer")
    }
    return value
}

private fun LegacyKeyProblemsSource.issue(
    issueId: String?,
    code: KeyProblemsMigrationIssueCode,
    detail: String,
) = KeyProblemsMigrationIssue(contextId, issueId, code, detail)
