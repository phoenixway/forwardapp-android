package com.romankozak.forwardappmobile.core.data.models.entities

import com.google.gson.annotations.SerializedName

object ContextStatusValues {
    const val NO_PLAN = "NO_PLAN"
    const val PLANNING = "PLANNING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val ON_HOLD = "ON_HOLD"
    const val PAUSED = "PAUSED"

    fun getDisplayName(status: String?): String {
        return when (status) {
            NO_PLAN -> "Без плану"
            PLANNING -> "Планується"
            IN_PROGRESS -> "В реалізації"
            COMPLETED -> "Завершено"
            ON_HOLD -> "Відкладено"
            PAUSED -> "На паузі"
            else -> "Без плану"
        }
    }
}

object ScoringStatusValues {
    const val NOT_ASSESSED = "NOT_ASSESSED"
    const val IMPOSSIBLE_TO_ASSESS = "IMPOSSIBLE_TO_ASSESS"
    const val ASSESSED = "ASSESSED"
}

object ContextLogLevelValues {
    const val DETAILED = "DETAILED"
    const val NORMAL = "NORMAL"
}

object ContextLogEntryTypeValues {
    const val STATUS_CHANGE = "STATUS_CHANGE"
    const val COMMENT = "COMMENT"
    const val AUTOMATIC = "AUTOMATIC"
    const val INSIGHT = "INSIGHT"
    const val MILESTONE = "MILESTONE"
}

object BacklogItemTypeValues {
    const val GOAL = "GOAL"
    const val SUBLIST = "SUBLIST"
    const val LINK_ITEM = "LINK_ITEM"
    const val NOTE = "NOTE"
    const val NOTE_DOCUMENT = "NOTE_DOCUMENT"
    const val CHECKLIST = "CHECKLIST"
    const val SCRIPT = "SCRIPT"
    const val CONTEXT = "CONTEXT"
}

data class RelatedLink(
    @SerializedName("type") val type: LinkType?,
    @SerializedName("target") val target: String,
    @SerializedName("displayName") val displayName: String? = null,
)

enum class ContextViewMode {
    BACKLOG,
    INBOX,
    ADVANCED,
    ATTACHMENTS,
    DASHBOARD,
    DIRECTION,
    LOG,
    ARTIFACT,
    NOTES,
    VET_CASE,
}

enum class LinkType { CONTEXT, URL, OBSIDIAN }

enum class DayStatus { PLANNED, IN_PROGRESS, COMPLETED, MISSED, ARCHIVED }

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    NONE,
    ;

    fun getDisplayName(): String {
        return when (this) {
            LOW -> "Низький"
            MEDIUM -> "Середній"
            HIGH -> "Високий"
            CRITICAL -> "Критичний"
            NONE -> "Немає"
        }
    }
}

enum class TaskStatus { NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, DEFERRED}
