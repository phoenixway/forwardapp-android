package com.romankozak.forwardappmobile.core.context

object SystemContexts {
    val PERSONAL_MANAGEMENT = ContextId("personal-management")
    val STRATEGIC = ContextId("strategic")
    val STRATEGIC_BEACONS = ContextId("strategic-beacons")
    val MISSION = ContextId("mission")
    val LONG_TERM_STRATEGY = ContextId("long-term-strategy")
    val STRATEGIC_PROGRAMS = ContextId("strategic-programs")
    val MEDIUM_TERM_STRATEGY = ContextId("medium-term-strategy")
    val ACTIVE_QUESTS = ContextId("active-quests")
    val WEEK = ContextId("week")
    val INBOX = ContextId("inbox")
    val STRATEGIC_INBOX = ContextId("strategic-inbox")
    val STRATEGIC_REVIEW = ContextId("strategic-review")
    val MAIN_BEACONS = ContextId("main-beacons")
    val TODAY = ContextId("today")

    private val RESERVED = setOf(
        PERSONAL_MANAGEMENT,
        STRATEGIC,
        STRATEGIC_BEACONS,
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
        TODAY
    )

    fun isSystem(id: ContextId): Boolean = RESERVED.contains(id)
}