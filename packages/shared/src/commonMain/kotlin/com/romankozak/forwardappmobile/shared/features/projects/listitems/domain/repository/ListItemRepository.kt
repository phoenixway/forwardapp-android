package com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.repository

import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import kotlinx.coroutines.flow.Flow

interface ListItemRepository {
    fun getListItems(projectId: String): Flow<List<ListItem>>
}
