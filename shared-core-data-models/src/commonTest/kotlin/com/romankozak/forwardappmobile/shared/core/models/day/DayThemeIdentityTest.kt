package com.romankozak.forwardappmobile.shared.core.models.day

import kotlin.test.Test
import kotlin.test.assertEquals

class DayThemeIdentityTest {
    @Test
    fun `canonical DayTheme id matches frozen shared vectors`() {
        val vectors =
            listOf(
                Triple("day-1", "theme-1", "day_theme:5:day-1:7:theme-1"),
                Triple("a:b", "x:y:z", "day_theme:3:a:b:5:x:y:z"),
                Triple("день", "тема", "day_theme:4:день:4:тема"),
                Triple("day🙂", "theme🧠", "day_theme:5:day🙂:7:theme🧠"),
            )

        vectors.forEach { (dayPlanId, themeId, expected) ->
            assertEquals(expected, canonicalDayThemeId(dayPlanId, themeId))
        }
    }
}
