package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingMode
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingRule
import com.romankozak.forwardappmobile.shared.core.domain.workspace.WorkspaceSortingTarget

/** Compatibility projection for the existing free-text settings UI. */
internal object InboxSortingLegacyTextAdapter {
    fun decode(raw: String): InboxSortingCapabilityConfigurationV1 =
        InboxSortingCapabilityConfigurationV1(
            buildList {
                resolve(raw, listOf("backlog"), WorkspaceSortingTarget.BACKLOG)?.let(::add)
                resolve(raw, listOf("inbox"), WorkspaceSortingTarget.INBOX)?.let(::add)
                resolve(raw, listOf("connections", "attachments"), WorkspaceSortingTarget.CONNECTIONS)?.let(::add)
            },
        )

    fun encode(configuration: InboxSortingCapabilityConfigurationV1): String =
        configuration.rules.joinToString(separator = "\n") { rule ->
            "${rule.target.legacyKey()}:${rule.mode.name.lowercase()}"
        }

    private fun resolve(
        raw: String,
        keys: List<String>,
        target: WorkspaceSortingTarget,
    ): WorkspaceSortingRule? {
        keys.forEach { key ->
            val pattern = Regex("""(?im)^\s*${Regex.escape(key)}\s*:\s*([a-z_]+)\s*$""")
            val modeName = pattern.find(raw)?.groupValues?.getOrNull(1) ?: return@forEach
            val mode = WorkspaceSortingMode.entries.firstOrNull { it.name.equals(modeName, ignoreCase = true) }
            if (mode != null && isAllowed(target, mode)) return WorkspaceSortingRule(target, mode)
        }
        return null
    }

    private fun isAllowed(target: WorkspaceSortingTarget, mode: WorkspaceSortingMode): Boolean =
        when (target) {
            WorkspaceSortingTarget.BACKLOG -> mode in setOf(WorkspaceSortingMode.NEWEST, WorkspaceSortingMode.OLDEST)
            WorkspaceSortingTarget.INBOX -> mode != WorkspaceSortingMode.TYPE
            WorkspaceSortingTarget.CONNECTIONS -> true
        }

    private fun WorkspaceSortingTarget.legacyKey(): String =
        when (this) {
            WorkspaceSortingTarget.BACKLOG -> "backlog"
            WorkspaceSortingTarget.INBOX -> "inbox"
            WorkspaceSortingTarget.CONNECTIONS -> "connections"
        }
}
