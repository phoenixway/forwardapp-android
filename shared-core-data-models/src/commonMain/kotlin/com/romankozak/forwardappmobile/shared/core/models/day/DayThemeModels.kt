package com.romankozak.forwardappmobile.shared.core.models.day

import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta

/**
 * Canonical global Theme definition.
 *
 * Descriptive fields remain populated even when isDeleted=true so historical
 * DayThemes can still resolve and render their ThemeDefinition tombstone.
 * Deleted or archived definitions must not be used for new materialization.
 */
data class ThemeDefinition(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val title: String,
    val colorArgb: Long,
    val iconKey: String,
    val description: String,
    val carryForward: Boolean,
    val archived: Boolean,
) : SyncEntityMeta

/**
 * Canonical daily materialization of one ThemeDefinition on one DayPlan.
 *
 * Global descriptive state belongs only to ThemeDefinition. This entity owns
 * daily state only. Its logical uniqueness key is (dayPlanId, themeId), and
 * id must equal canonicalDayThemeId(dayPlanId, themeId).
 */
data class DayTheme(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val themeId: String,
    val dayPlanId: String,
    val budgetPercent: Int,
    val order: Long,
    val isActive: Boolean,
) : SyncEntityMeta
