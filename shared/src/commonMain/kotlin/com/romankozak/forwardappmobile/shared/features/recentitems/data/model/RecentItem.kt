package com.romankozak.forwardappmobile.shared.features.recentitems.data.model

data class RecentItem(
    val id: String,
    val type: RecentItemType,
    val lastAccessed: Long,
    val displayName: String,
    val target: String,
    val isPinned: Boolean = false,
)
