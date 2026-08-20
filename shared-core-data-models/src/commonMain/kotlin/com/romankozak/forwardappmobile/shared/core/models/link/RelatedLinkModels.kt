package com.romankozak.forwardappmobile.shared.core.models.link

enum class CanonicalLinkType {
    CONTEXT,
    NOTE_DOCUMENT,
    JOURNAL_DOCUMENT,
    CHECKLIST,
    MUSIC_NOTE,
    URL,
    OBSIDIAN,
}

data class CanonicalRelatedLink(
    val type: CanonicalLinkType?,
    val target: String,
    val displayName: String? = null,
    val vault: String? = null,
)
