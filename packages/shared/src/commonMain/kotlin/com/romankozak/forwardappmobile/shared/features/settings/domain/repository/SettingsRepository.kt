package com.romankozak.forwardappmobile.shared.features.settings.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val customContextNamesFlow: Flow<Set<String>>
    fun getContextTagFlow(contextKey: String): Flow<String>
    fun getContextEmojiFlow(emojiKey: String): Flow<String>
    fun getCustomContextTagFlow(name: String): Flow<String>
    fun getCustomContextEmojiFlow(name: String): Flow<String>

    companion object {
        val reservedContexts = mapOf(
            "BUY" to ("context_tag_buy" to "context_emoji_buy"),
            "PM" to ("context_tag_pm" to "context_emoji_pm"),
            "PAPER" to ("context_tag_paper" to "context_emoji_paper"),
            "MENTAL" to ("context_tag_mental" to "context_emoji_mental"),
            "PROVIDENCE" to ("context_tag_providence" to "context_emoji_providence"),
            "MANUAL" to ("context_tag_manual" to "context_emoji_manual"),
            "RESEARCH" to ("context_tag_research" to "context_emoji_research"),
            "DEVICE" to ("context_tag_device" to "context_emoji_device"),
            "MIDDLE" to ("context_tag_middle" to "context_emoji_middle"),
            "LONG" to ("context_tag_long" to "context_emoji_long")
        )
    }
}
