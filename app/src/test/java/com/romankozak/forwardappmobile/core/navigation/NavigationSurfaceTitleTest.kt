package com.romankozak.forwardappmobile.core.navigation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import com.romankozak.forwardappmobile.data.database.models.NavigationType
import org.junit.Test

class NavigationSurfaceTitleTest {
    @Test fun `new root entry uses Navigation with unchanged identity and route`() {
        val entry = NavigationEntry.createProjectHierarchyScreen()
        assertThat(entry.title).isEqualTo("Navigation")
        assertThat(entry.id).isEqualTo("main")
        assertThat(entry.route).isEqualTo("goal_lists_screen")
    }

    @Test fun `retained old history label is displayed as Navigation without mutation`() {
        val entry = NavigationEntry.createProjectHierarchyScreen().copy(title = "Big Picture")
        assertThat(entry.navigationSurfaceTitle()).isEqualTo("Navigation")
        assertThat(entry.title).isEqualTo("Big Picture")
        assertThat(entry.route).isEqualTo("goal_lists_screen")
    }

    @Test fun `custom hierarchy titles and unrelated entity names are not renamed`() {
        val entry = NavigationEntry.createProjectHierarchyScreen().copy(title = "Health")
        assertThat(entry.navigationSurfaceTitle()).isEqualTo("Health")
        assertThat(entry.copy(type = NavigationType.PROJECT_SCREEN, title = "Big Picture").navigationSurfaceTitle())
            .isEqualTo("Big Picture")
    }
}
