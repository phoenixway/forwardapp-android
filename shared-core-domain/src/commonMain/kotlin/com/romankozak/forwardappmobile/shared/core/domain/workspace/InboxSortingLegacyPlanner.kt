package com.romankozak.forwardappmobile.shared.core.domain.workspace

data class LegacyInboxSortingSource(
    val contextId: String,
    val rulesText: String,
    val updatedAt: Long,
)

data class InboxSortingMigrationBindings(
    val workspaceIdByContextId: Map<String, String>,
    val capabilityInstanceIdByWorkspaceId: Map<String, String>,
)

enum class InboxSortingMigrationIssueCode {
    INVALID_RULE,
    UNKNOWN_TARGET,
    UNKNOWN_MODE,
    DUPLICATE_TARGET,
    MULTIPLE_SOURCES_FOR_WORKSPACE,
    UNRESOLVED_OWNER_WORKSPACE,
    UNRESOLVED_CAPABILITY_INSTANCE,
}

data class InboxSortingMigrationIssue(
    val contextId: String,
    val lineNumber: Int?,
    val code: InboxSortingMigrationIssueCode,
    val detail: String,
)

data class InboxSortingConfigurationUpdate(
    val workspaceId: String,
    val capabilityInstanceId: String,
    val configurationVersion: Int,
    val configuration: String,
    val sourceUpdatedAt: Long,
)

data class InboxSortingMigrationPlan(
    val sourceCount: Int,
    val updates: List<InboxSortingConfigurationUpdate>,
    val issues: List<InboxSortingMigrationIssue>,
) {
    val canApply: Boolean
        get() = issues.isEmpty()

    val isFullyAccounted: Boolean
        get() = canApply && sourceCount == updates.size
}

object InboxSortingLegacyPlanner {
    private val linePattern = Regex("""^\s*([a-zA-Z_]+)\s*:\s*([a-zA-Z_]+)\s*$""")

    fun plan(
        sources: List<LegacyInboxSortingSource>,
        bindings: InboxSortingMigrationBindings,
    ): InboxSortingMigrationPlan {
        val issues = mutableListOf<InboxSortingMigrationIssue>()
        addDuplicateOwnerDiagnostics(sources, bindings, issues)
        val updates =
            sources.mapNotNull { source ->
                val workspaceId = bindings.workspaceIdByContextId[source.contextId]
                if (workspaceId == null) {
                    issues += source.issue(null, InboxSortingMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE, "No proven Workspace owner")
                    return@mapNotNull null
                }
                val capabilityId = bindings.capabilityInstanceIdByWorkspaceId[workspaceId]
                if (capabilityId == null) {
                    issues += source.issue(null, InboxSortingMigrationIssueCode.UNRESOLVED_CAPABILITY_INSTANCE, "No INBOX_SORTING instance")
                    return@mapNotNull null
                }

                val rules = parseRules(source, issues)
                val configuration = InboxSortingCapabilityConfigurationV1(rules)
                val encoded = runCatching { InboxSortingCapabilityConfigurationCodec.encode(configuration) }.getOrNull()
                if (encoded == null) return@mapNotNull null
                InboxSortingConfigurationUpdate(
                    workspaceId = workspaceId,
                    capabilityInstanceId = capabilityId,
                    configurationVersion = InboxSortingCapabilityConfigurationCodec.CURRENT_VERSION,
                    configuration = encoded,
                    sourceUpdatedAt = source.updatedAt,
                )
            }
        return InboxSortingMigrationPlan(sources.size, updates, issues.distinct())
    }

    private fun addDuplicateOwnerDiagnostics(
        sources: List<LegacyInboxSortingSource>,
        bindings: InboxSortingMigrationBindings,
        issues: MutableList<InboxSortingMigrationIssue>,
    ) {
        sources.groupBy { bindings.workspaceIdByContextId[it.contextId] }
            .filterKeys { it != null }
            .filterValues { it.size > 1 }
            .forEach { (workspaceId, duplicates) ->
                duplicates.forEach { source ->
                    issues +=
                        source.issue(
                            lineNumber = null,
                            code = InboxSortingMigrationIssueCode.MULTIPLE_SOURCES_FOR_WORKSPACE,
                            detail = "Multiple legacy policy rows resolve to Workspace $workspaceId",
                        )
                }
            }
    }

    private fun parseRules(
        source: LegacyInboxSortingSource,
        issues: MutableList<InboxSortingMigrationIssue>,
    ): List<WorkspaceSortingRule> {
        val parsed = mutableListOf<WorkspaceSortingRule>()
        source.rulesText.lineSequence().forEachIndexed { index, rawLine ->
            if (rawLine.isBlank()) return@forEachIndexed
            val match = linePattern.matchEntire(rawLine)
            if (match == null) {
                issues += source.issue(index + 1, InboxSortingMigrationIssueCode.INVALID_RULE, "Expected target:mode")
                return@forEachIndexed
            }

            val target = parseTarget(match.groupValues[1])
            if (target == null) {
                issues += source.issue(index + 1, InboxSortingMigrationIssueCode.UNKNOWN_TARGET, "Unknown target ${match.groupValues[1]}")
                return@forEachIndexed
            }
            val mode = parseMode(match.groupValues[2])
            if (mode == null || !isAllowed(target, mode)) {
                issues +=
                    source.issue(
                        index + 1,
                        InboxSortingMigrationIssueCode.UNKNOWN_MODE,
                        "Unsupported mode ${match.groupValues[2]} for $target",
                    )
                return@forEachIndexed
            }
            if (parsed.any { it.target == target }) {
                issues += source.issue(index + 1, InboxSortingMigrationIssueCode.DUPLICATE_TARGET, "Target $target occurs more than once")
                return@forEachIndexed
            }
            parsed += WorkspaceSortingRule(target, mode)
        }
        return parsed
    }

    private fun parseTarget(raw: String): WorkspaceSortingTarget? =
        when (raw.trim().lowercase()) {
            "backlog" -> WorkspaceSortingTarget.BACKLOG
            "inbox" -> WorkspaceSortingTarget.INBOX
            "connections", "attachments" -> WorkspaceSortingTarget.CONNECTIONS
            else -> null
        }

    private fun parseMode(raw: String): WorkspaceSortingMode? =
        WorkspaceSortingMode.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }

    private fun isAllowed(
        target: WorkspaceSortingTarget,
        mode: WorkspaceSortingMode,
    ): Boolean =
        runCatching {
            InboxSortingCapabilityConfigurationCodec.encode(
                InboxSortingCapabilityConfigurationV1(listOf(WorkspaceSortingRule(target, mode))),
            )
        }.isSuccess
}

private fun LegacyInboxSortingSource.issue(
    lineNumber: Int?,
    code: InboxSortingMigrationIssueCode,
    detail: String,
) = InboxSortingMigrationIssue(contextId, lineNumber, code, detail)
