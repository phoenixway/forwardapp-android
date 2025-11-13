package com.romankozak.forwardappmobile.shared.features.recent.data.mappers

import com.romankozak.forwardappmobile.shared.features.recent.RecentItems
import com.romankozak.forwardappmobile.shared.features.recent.domain.model.RecentItem

fun RecentItems.toDomain(): RecentItem =
    RecentItem(
        id = id,
        type = type,
        lastAccessed = lastAccessed,
        displayName = displayName,
        target = target,
        isPinned = isPinned,
    )

fun RecentItem.toDb(): RecentItems =
    RecentItems(
        id = id,
        type = type,
        lastAccessed = lastAccessed,
        displayName = displayName,
        target = target,
        isPinned = isPinned,
    )
