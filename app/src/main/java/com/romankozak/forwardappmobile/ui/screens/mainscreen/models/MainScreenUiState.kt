package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData

data class MainScreenUiState(
    val projectHierarchy: ListHierarchyData = ListHierarchyData(),
    val isReadyForFiltering: Boolean = false,
)