package com.romankozak.forwardappmobile.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local-only marker for one-time legacy -> canonical Day Theme persistence bootstrap. */
@Entity(tableName = "day_theme_canonical_bootstrap_state")
data class DayThemeCanonicalBootstrapStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val version: Int,
    val completedAt: Long,
) {
    companion object {
        const val SINGLETON_ID: Int = 1
    }
}
