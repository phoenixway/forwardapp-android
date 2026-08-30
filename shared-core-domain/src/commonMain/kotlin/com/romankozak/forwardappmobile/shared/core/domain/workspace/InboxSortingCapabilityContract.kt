package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class WorkspaceSortingTarget {
    BACKLOG,
    INBOX,
    CONNECTIONS,
}

enum class WorkspaceSortingMode {
    NEWEST,
    OLDEST,
    ALPHA,
    TYPE,
}

data class WorkspaceSortingRule(
    val target: WorkspaceSortingTarget,
    val mode: WorkspaceSortingMode,
)

data class InboxSortingCapabilityConfigurationV1(
    val rules: List<WorkspaceSortingRule>,
)

object InboxSortingCapabilityConfigurationCodec : CapabilityConfigurationCodec {
    const val CURRENT_VERSION: Int = 1
    private val json = Json

    override val currentVersion: Int = CURRENT_VERSION

    override fun encodeDefault(): String = encode(InboxSortingCapabilityConfigurationV1(emptyList()))

    override fun validate(
        version: Int,
        raw: String,
    ) {
        decode(version, raw)
    }

    fun encode(configuration: InboxSortingCapabilityConfigurationV1): String {
        validateRules(configuration.rules)
        val rules =
            configuration.rules.joinToString(separator = ",") { rule ->
                "{\"target\":\"${rule.target.name}\",\"mode\":\"${rule.mode.name}\"}"
            }
        return "{\"rules\":[$rules]}"
    }

    fun decode(
        version: Int,
        raw: String,
    ): InboxSortingCapabilityConfigurationV1 {
        require(version == CURRENT_VERSION) {
            "Unsupported INBOX_SORTING configuration version: $version"
        }
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonObject
        require(root != null && root.keys == setOf("rules")) {
            "Invalid INBOX_SORTING configuration v1"
        }
        val array = root["rules"] as? JsonArray
        require(array != null) { "INBOX_SORTING rules must be an array" }
        val rules = array.map(::decodeRule)
        validateRules(rules)
        return InboxSortingCapabilityConfigurationV1(rules)
    }

    private fun decodeRule(element: kotlinx.serialization.json.JsonElement): WorkspaceSortingRule {
        val rule = element as? JsonObject
        require(rule != null && rule.keys == setOf("target", "mode")) {
            "Invalid INBOX_SORTING rule"
        }
        val targetName = (rule["target"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        val modeName = (rule["mode"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        val target = WorkspaceSortingTarget.entries.firstOrNull { it.name == targetName }
        val mode = WorkspaceSortingMode.entries.firstOrNull { it.name == modeName }
        require(target != null && mode != null) { "Unknown INBOX_SORTING target or mode" }
        return WorkspaceSortingRule(target, mode)
    }
}

fun effectiveSortingMode(
    configuration: InboxSortingCapabilityConfigurationV1,
    target: WorkspaceSortingTarget,
): WorkspaceSortingMode =
    configuration.rules.firstOrNull { it.target == target }?.mode ?: WorkspaceSortingMode.NEWEST

fun requiredCapabilityForSorting(target: WorkspaceSortingTarget): WorkspaceCapabilityType =
    when (target) {
        WorkspaceSortingTarget.BACKLOG -> WorkspaceCapabilityType.BACKLOG
        WorkspaceSortingTarget.INBOX -> WorkspaceCapabilityType.INBOX
        WorkspaceSortingTarget.CONNECTIONS -> WorkspaceCapabilityType.CONNECTIONS
    }

private fun validateRules(rules: List<WorkspaceSortingRule>) {
    require(rules.map { it.target }.distinct().size == rules.size) {
        "INBOX_SORTING allows at most one rule per target"
    }
    rules.forEach { rule ->
        val allowed =
            when (rule.target) {
                WorkspaceSortingTarget.BACKLOG -> setOf(WorkspaceSortingMode.NEWEST, WorkspaceSortingMode.OLDEST)
                WorkspaceSortingTarget.INBOX ->
                    setOf(WorkspaceSortingMode.NEWEST, WorkspaceSortingMode.OLDEST, WorkspaceSortingMode.ALPHA)
                WorkspaceSortingTarget.CONNECTIONS -> WorkspaceSortingMode.entries.toSet()
            }
        require(rule.mode in allowed) {
            "${rule.mode} is not valid for ${rule.target}"
        }
    }
}
