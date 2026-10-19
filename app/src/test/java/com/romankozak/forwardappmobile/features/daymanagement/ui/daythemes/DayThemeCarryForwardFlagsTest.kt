package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import org.junit.Test

class DayThemeCarryForwardFlagsTest {
    @Test fun `new editor draft defaults one-off without changing compatibility default`() {
        assertThat(DayThemeDraft("Title", 1, "target", "", 0).carryForward).isFalse()
    }

    @Test fun `explicit one-off or carry choice persists on creation`() {
        for (carry in listOf(false, true)) {
            val plan = planCanonicalDayThemeRuntimeUpdate("day", 100, emptyList(), emptyList(), null,
                DayThemeDocument(themes = listOf(DayTheme(id = "theme", dayPlanId = "day", title = "Title", carryForward = carry))))
            assertThat(plan.themeDefinitions.single().carryForward).isEqualTo(carry)
        }
    }

    @Test fun `changing carry flag increments only its existing definition lifecycle`() {
        val definition = ThemeDefinitionSnapshot("theme", "Title", 1, "target", "", true, false, 1, 1, 1, 4, false)
        val initial = planCanonicalDayThemeRuntimeUpdate("day", 10, listOf(definition), emptyList(), null,
            DayThemeDocument(themes = listOf(DayTheme(id = "theme", dayPlanId = "day", title = "Title", colorArgb = 1))))
        val projected = projectCanonicalDayThemeDocument("day", listOf(definition), initial.dayThemes, null)
        assertThat(projected.themes.single().carryForward).isTrue()
        val changed = planCanonicalDayThemeRuntimeUpdate("day", 100, listOf(definition), initial.dayThemes, null,
            projected.copy(themes = projected.themes.map { it.copy(carryForward = false) }))
        assertThat(changed.themeDefinitions.single().carryForward).isFalse()
        assertThat(changed.themeDefinitions.single().version).isEqualTo(5)
        assertThat(changed.themeDefinitions.single().syncedAt).isNull()
        assertThat(changed.dayThemes).isEmpty()
    }
}
