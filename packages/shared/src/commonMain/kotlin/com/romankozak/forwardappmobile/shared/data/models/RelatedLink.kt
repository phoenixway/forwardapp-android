package com.romankozak.forwardappmobile.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class LinkType { PROJECT, URL, OBSIDIAN }

@Serializable
data class RelatedLink(
    val type: LinkType?,
    val target: String,
    val displayName: String? = null,
)
