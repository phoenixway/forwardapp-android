package com.romankozak.forwardappmobile.features.mainscreen.scopelinks

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult

data class ScopeAttachmentOption(
    val id: String,
    val name: String,
    val linkType: LinkType? = null,
)

fun AttachmentLibraryQueryResult.toScopeAttachmentOption(): ScopeAttachmentOption {
    val relatedLink =
        linkDisplayName?.let { json ->
            runCatching { Gson().fromJson(json, RelatedLink::class.java) }.getOrNull()
        }
    val label =
        noteName
            ?: checklistName
            ?: contextName
            ?: relatedLink?.displayName
            ?: relatedLink?.target
            ?: "Attachment ${id.takeLast(4)}"

    return ScopeAttachmentOption(id = id, name = label, linkType = relatedLink?.type)
}
