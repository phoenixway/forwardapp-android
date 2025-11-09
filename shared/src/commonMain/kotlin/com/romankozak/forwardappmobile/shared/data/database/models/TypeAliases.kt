package com.romankozak.forwardappmobile.shared.data.database.models

import kotlinx.serialization.Serializable

typealias RelatedLinkList = List<RelatedLink>

@Serializable
data class RelatedLink(
    val url: String,
    val title: String? = null
)
