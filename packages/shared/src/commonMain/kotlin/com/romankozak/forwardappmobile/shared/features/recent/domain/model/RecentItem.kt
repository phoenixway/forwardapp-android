package com.romankozak.forwardappmobile.shared.features.recent.domain.model

data class RecentItem(
    val id: String,
    val type: String,
    val lastAccessed: Long,
    val displayName: String,
    val target: String,
    val isPinned: Boolean,
)
