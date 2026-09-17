package com.romankozak.forwardappmobile.core.context

/**
 * Factory defaults for reserved operational identities.
 *
 * These values seed missing canonical System Workspaces. Historical same-id
 * Context rows may provide one-time migration metadata, but no reserved Context
 * compatibility shell is created from these definitions.
 */
data class SystemOperationalDefinition(
    val id: String,
    val defaultName: String,
    val defaultParentId: String?,
)

object SystemOperationalDefinitions {
    /** Parent-before-child order retained for deterministic canonical seeding. */
    val all: List<SystemOperationalDefinition> =
        listOf(
            SystemOperationalDefinition(
                id = SystemContexts.PERSONAL_MANAGEMENT.raw,
                defaultName = "personal-management",
                defaultParentId = null,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.STRATEGIC.raw,
                defaultName = "strategic",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.LEVELS.raw,
                defaultName = "levels",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.WEEK.raw,
                defaultName = "week",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.TODAY.raw,
                defaultName = "day-management",
                defaultParentId = SystemContexts.LEVELS.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.ABOUT_MODES.raw,
                defaultName = "mode-about",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.SESSION_IMPROVE.raw,
                defaultName = "mode-improve",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.SESSION_EXECUTION.raw,
                defaultName = "mode-execution",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.SESSION_CONTROL.raw,
                defaultName = "mode-control",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.SESSION_RECOVERY.raw,
                defaultName = "mode-recovery",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.SESSION_EMERGENCY.raw,
                defaultName = "mode-emergency",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.MAIN_BEACONS.raw,
                defaultName = "main-beacons",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.MISSION.raw,
                defaultName = "mission",
                defaultParentId = SystemContexts.STRATEGIC.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.LONG_TERM_STRATEGY.raw,
                defaultName = "long-term-strategy",
                defaultParentId = SystemContexts.STRATEGIC.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.STRATEGIC_PROGRAMS.raw,
                defaultName = "strategic-programs",
                defaultParentId = SystemContexts.STRATEGIC.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.MEDIUM_TERM_STRATEGY.raw,
                defaultName = "medium-term-strategy",
                defaultParentId = SystemContexts.PERSONAL_MANAGEMENT.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.ACTIVE_QUESTS.raw,
                defaultName = "active-quests",
                defaultParentId = SystemContexts.WEEK.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.STRATEGIC_INBOX.raw,
                defaultName = "strategic-inbox",
                defaultParentId = SystemContexts.STRATEGIC.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.STRATEGIC_REVIEW.raw,
                defaultName = "strategic-review",
                defaultParentId = SystemContexts.STRATEGIC.raw,
            ),
            SystemOperationalDefinition(
                id = SystemContexts.INBOX.raw,
                defaultName = "inbox",
                defaultParentId = SystemContexts.TODAY.raw,
            ),
        )
}
