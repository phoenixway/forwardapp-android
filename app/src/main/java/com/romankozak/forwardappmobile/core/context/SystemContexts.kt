package com.romankozak.forwardappmobile.core.context

object SystemContexts {
    val PERSONAL_MANAGEMENT = ContextId("sys_personal-management")
    val STRATEGIC = ContextId("sys_strategic")
    val MISSION = ContextId("sys_mission")
    val LONG_TERM_STRATEGY = ContextId("sys_long-term-strategy")
    val STRATEGIC_PROGRAMS = ContextId("sys_strategic-programs")
    val MEDIUM_TERM_STRATEGY = ContextId("sys_medium-term-strategy")
    val ACTIVE_QUESTS = ContextId("sys_active-quests")
    val WEEK = ContextId("sys_week")
    val INBOX = ContextId("sys_inbox")
    val STRATEGIC_INBOX = ContextId("sys_strategic-inbox")
    val STRATEGIC_REVIEW = ContextId("sys_strategic-review")
    val MAIN_BEACONS = ContextId("sys_main-beacons")
    val TODAY = ContextId("sys_today")

    private val RESERVED =
        setOf(
            PERSONAL_MANAGEMENT,
            STRATEGIC,
            MISSION,
            LONG_TERM_STRATEGY,
            STRATEGIC_PROGRAMS,
            MEDIUM_TERM_STRATEGY,
            ACTIVE_QUESTS,
            WEEK,
            INBOX,
            STRATEGIC_INBOX,
            STRATEGIC_REVIEW,
            MAIN_BEACONS,
            TODAY,
        )

    private val PINNED_ROOT =
        setOf(
            PERSONAL_MANAGEMENT,
        )

    fun isSystem(id: ContextId): Boolean = RESERVED.contains(id)

    fun isPinnedRoot(id: ContextId): Boolean = PINNED_ROOT.contains(id)
}
