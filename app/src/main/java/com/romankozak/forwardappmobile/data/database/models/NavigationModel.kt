package com.romankozak.forwardappmobile.data.database.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NavigationEntry(
    val type: NavigationType,
    val id: String,
    val title: String,
    val route: String,
    val timestamp: Long = System.currentTimeMillis(),
) : Parcelable {
    companion object {
        fun createMainScreen(): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.MAIN_SCREEN,
                id = "main",
                title = "Projects",
                route = "goal_lists_screen",
            )
        }

        fun createProjectScreen(
            projectId: String,
            projectName: String,
        ): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.PROJECT_SCREEN,
                id = projectId,
                title = projectName,
                route = "goal_detail_screen/$projectId",
            )
        }

        fun createGlobalSearch(query: String): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.GLOBAL_SEARCH,
                id = "search_$query",
                title = "Search: $query",
                route = "global_search_screen/$query",
            )
        }
    }
}


@Parcelize
enum class NavigationType : Parcelable {
    MAIN_SCREEN,
    PROJECT_SCREEN,
    GLOBAL_SEARCH,
    SETTINGS,
    EDIT_PROJECT,
}
