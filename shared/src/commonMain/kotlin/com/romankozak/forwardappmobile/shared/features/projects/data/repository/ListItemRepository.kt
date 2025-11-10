package com.romankozak.forwardappmobile.shared.features.projects.data.repository

import com.romankozak.forwardappmobile.shared.features.projects.data.models.ListItem
import kotlinx.coroutines.flow.Flow

interface ListItemRepository {
    fun getListItems(projectId: String): Flow<List<ListItem>>
}
