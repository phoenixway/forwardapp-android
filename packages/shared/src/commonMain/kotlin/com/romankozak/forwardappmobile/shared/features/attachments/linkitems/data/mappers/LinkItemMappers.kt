package com.romankozak.forwardappmobile.shared.features.attachments.linkitems.data.mappers

import com.romankozak.forwardappmobile.shared.data.database.models.GlobalLinkSearchResult
import com.romankozak.forwardappmobile.shared.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.LinkItems
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.SearchLinkItems

fun LinkItems.toDomain(): LinkItemEntity =
    LinkItemEntity(
        id = id,
        linkData = linkData,
        createdAt = createdAt,
    )

fun SearchLinkItems.toSearchResult(): GlobalLinkSearchResult =
    GlobalLinkSearchResult(
        link = LinkItemEntity(
            id = id,
            linkData = linkData,
            createdAt = createdAt,
        ),
        projectId = projectId,
        projectName = projectName,
        listItemId = listItemId,
        pathSegments = projectPath?.split(" / ")?.filter { it.isNotBlank() } ?: emptyList(),
    )
