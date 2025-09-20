package com.romankozak.forwardappmobile.data.database.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NavigationEntry(
    val type: NavigationType,
    val id: String, // Unique ID for the screen (e.g., projectId, "main", "search_query")
    val title: String, // Display title for the history menu (e.g., project name)
    val route: String, // The full route for Jetpack Navigation
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    companion object {
        fun createMainScreen(): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.MAIN_SCREEN,
                id = "main",
                title = "Projects",
                route = "goal_lists_screen"
            )
        }

        fun createProjectScreen(projectId: String, projectName: String): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.PROJECT_SCREEN,
                id = projectId,
                title = projectName,
                route = "goal_detail_screen/$projectId"
            )
        }

        fun createGlobalSearch(query: String): NavigationEntry {
            return NavigationEntry(
                type = NavigationType.GLOBAL_SEARCH,
                id = "search_$query",
                title = "Search: $query",
                route = "global_search_screen/$query"
            )
        }
    }
}

/**
 * Defines the types of screens that can be part of the navigation history.
 */
@Parcelize
enum class NavigationType : Parcelable {
    MAIN_SCREEN,
    PROJECT_SCREEN,
    GLOBAL_SEARCH,
    SETTINGS,
    EDIT_PROJECT
}