package com.romankozak.forwardappmobile.core.data.models.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_state_intervals",
    indices = [
        Index(value = ["endedAt"]),
        Index(value = ["startedAt"]),
    ],
)
data class UserStateIntervalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val stateType: String,
    val crisisLevel: Int? = null,
    val label: String? = null,
    val source: String = "MANUAL",
    val createdFromActivityId: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
)
