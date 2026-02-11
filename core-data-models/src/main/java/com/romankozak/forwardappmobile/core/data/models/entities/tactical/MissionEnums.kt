package com.romankozak.forwardappmobile.core.data.models.entities.tactical

import java.util.Locale

enum class MissionStatus {
    ACTIVE,
    INACTIVE,
    PAUSED,
    COMPLETED,
    ;

    companion object {
        fun fromRaw(raw: String?): MissionStatus {
            return when (raw?.trim()?.uppercase(Locale.ROOT)) {
                ACTIVE.name,
                "IN_PROGRESS",
                -> ACTIVE
                INACTIVE.name,
                "PENDING",
                -> INACTIVE
                PAUSED.name,
                "OVERDUE",
                -> PAUSED
                COMPLETED.name -> COMPLETED
                else -> ACTIVE
            }
        }
    }
}

enum class MissionPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
