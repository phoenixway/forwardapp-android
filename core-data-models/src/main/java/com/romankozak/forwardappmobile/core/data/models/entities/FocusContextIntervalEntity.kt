package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_context_intervals",
    indices = [
        Index(value = ["contextId"]),
        Index(value = ["scope"]),
        Index(value = ["endedAt"]),
        Index(value = ["startedAt"]),
    ],
)
data class FocusContextIntervalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contextId: String,
    val scope: String = SCOPE_GLOBAL,
    val priority: Int? = null,
    val source: String = SOURCE_MANUAL,
    val createdFromActivityId: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
) {
    companion object {
        const val SCOPE_GLOBAL = "GLOBAL"
        const val SOURCE_MANUAL = "MANUAL"
    }
}
