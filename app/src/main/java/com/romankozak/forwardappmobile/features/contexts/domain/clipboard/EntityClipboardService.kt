package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ClipboardOperation {
    COPY,
    CUT,
}

enum class ClipboardEntityType {
    BACKLOG_GOAL,
    BACKLOG_CONTEXT_LINK,
    BACKLOG_ITEM,
    BACKLOG_ATTACHMENT,
    DIRECTION_ITEM,
    CHECKLIST_ITEM,
    DAY_TASK,
    TACTICAL_MISSION,
}

sealed interface ClipboardEntityRef {
    val type: ClipboardEntityType

    data class BacklogGoal(
        val goalId: String,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.BACKLOG_GOAL
    }

    data class BacklogContextLink(
        val contextId: String,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.BACKLOG_CONTEXT_LINK
    }

    data class BacklogItem(
        val listItemId: String,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.BACKLOG_ITEM
    }

    data class BacklogAttachment(
        val listItemId: String,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.BACKLOG_ATTACHMENT
    }

    data class DirectionItem(
        val directionItemId: String,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.DIRECTION_ITEM
    }

    data class ChecklistItem(
        val checklistItemId: String,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.CHECKLIST_ITEM
    }

    data class DayTask(
        val taskId: String,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.DAY_TASK
    }

    data class TacticalMission(
        val missionId: Long,
    ) : ClipboardEntityRef {
        override val type: ClipboardEntityType = ClipboardEntityType.TACTICAL_MISSION
    }
}

data class EntityClipboardPayload(
    val id: String = UUID.randomUUID().toString(),
    val sourceContextId: String,
    val operation: ClipboardOperation,
    val entities: List<ClipboardEntityRef>,
    val createdAt: Long = System.currentTimeMillis(),
)

@Singleton
class EntityClipboardService
    @Inject
    constructor() {
        private val _payload = MutableStateFlow<EntityClipboardPayload?>(null)
        val payload: StateFlow<EntityClipboardPayload?> = _payload.asStateFlow()

        fun set(value: EntityClipboardPayload) {
            _payload.value = value
        }

        fun clear() {
            _payload.value = null
        }
    }
