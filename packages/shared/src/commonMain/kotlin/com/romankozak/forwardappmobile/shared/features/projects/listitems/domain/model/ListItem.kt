package com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model

import kotlinx.serialization.Serializable
import com.romankozak.forwardappmobile.shared.data.models.ListItemTypeValues

@Serializable
data class ListItem(
    val id: String,
    val projectId: String,
    val itemType: String,
    val entityId: String,
    val itemOrder: Long,
)
