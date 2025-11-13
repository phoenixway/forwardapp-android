package com.romankozak.forwardappmobile.shared.data.database.models

data class LinkItemEntity(
    val id: String,
    val linkData: RelatedLink,
    val createdAt: Long,
)
