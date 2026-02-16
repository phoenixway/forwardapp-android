package com.romankozak.forwardappmobile.domain.userawareness

enum class UserAwarenessStateType {
    NORMAL,
    CRISIS,
    EXHAUSTION,
    UNPRODUCTIVE,
}

enum class UserStateSource {
    MANUAL,
    SUGGESTED,
}

data class UserStateChange(
    val type: UserAwarenessStateType,
    val crisisLevel: Int? = null,
    val label: String? = null,
)

data class UserStateInterval(
    val id: Long,
    val type: UserAwarenessStateType,
    val crisisLevel: Int?,
    val label: String?,
    val source: UserStateSource,
    val createdFromActivityId: String?,
    val startedAt: Long,
    val endedAt: Long?,
)

data class NudgePolicyFlags(
    val allowGuiltMessaging: Boolean,
    val reduceNotifications: Boolean,
    val enableMicroActions: Boolean,
)

data class QuotaPolicy(
    val enforceTargetAndCap: Boolean,
    val minimumIsSoft: Boolean,
    val targetMultiplier: Float,
    val disablePushes: Boolean,
)

data class WeeklyReviewFlags(
    val turbulent: Boolean,
)

data class ContextStateMinutes(
    val contextId: String,
    val normalMinutes: Int = 0,
    val crisisMinutes: Int = 0,
    val exhaustionMinutes: Int = 0,
    val unproductiveMinutes: Int = 0,
)
