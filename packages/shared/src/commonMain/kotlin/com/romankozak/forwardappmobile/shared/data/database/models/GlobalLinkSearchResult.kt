package com.romankozak.forwardappmobile.shared.data.database.models

data class GlobalLinkSearchResult(
    val link: LinkItemEntity,
    val projectId: String,
    val projectName: String,
    val listItemId: String,
    val pathSegments: List<String>,
)
