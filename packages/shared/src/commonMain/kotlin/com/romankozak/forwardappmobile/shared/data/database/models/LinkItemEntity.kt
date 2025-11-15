package com.romankozak.forwardappmobile.shared.data.database.models

import com.romankozak.forwardappmobile.shared.data.models.RelatedLink

data class LinkItemEntity(
    val id: String,
    val linkData: RelatedLink,
    val createdAt: Long,
)
