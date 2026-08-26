package com.romankozak.forwardappmobile.shared.core.models.link

import kotlin.js.JsExport

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
enum class CanonicalLinkType {
    CONTEXT,
    NOTE_DOCUMENT,
    JOURNAL_DOCUMENT,
    CHECKLIST,
    MUSIC_NOTE,
    URL,
    OBSIDIAN,
}

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class CanonicalRelatedLink(
    val type: CanonicalLinkType?,
    val target: String,
    val displayName: String? = null,
    val vault: String? = null,
)
