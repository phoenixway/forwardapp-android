package com.romankozak.forwardappmobile.features.activitytracker.entities

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord

data class ActivityEntityDescriptor(
    val link: ActivityEntityLink,
    val title: String,
    val typeLabel: String,
)

fun ActivityRecord.effectiveEntityLinks(): List<ActivityEntityLink> =
    buildList {
        addAll(entityLinks)
        contextId?.let { add(ActivityEntityLink(it, ActivityEntityType.CONTEXT)) }
        goalId?.let { add(ActivityEntityLink(it, ActivityEntityType.GOAL)) }
    }.distinctBy { link -> link.entityType to link.entityId }

fun ActivityEntityType.displayName(): String =
    when (this) {
        ActivityEntityType.DAY_TASK -> "Завдання"
        ActivityEntityType.DAY_FOCUS -> "Фокус"
        ActivityEntityType.DAY_RESPONSIBILITY -> "Зона відповідальності"
        ActivityEntityType.DAY_THEME -> "Тема"
        ActivityEntityType.CONTEXT -> "Контекст"
        ActivityEntityType.GOAL -> "Ціль"
    }
