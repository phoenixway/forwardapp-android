package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

enum class DayManagementTab(val title: String, val icon: ImageVector, val description: String) {
    DAY_START("Day Start", Icons.Outlined.PlayCircle, "Започаткувати день"),
    DAY_THEMES("Теми", Icons.Outlined.Palette, "Визначити мета-напрямки дня"),
    DAY_FOCUSES("Day Focuses", Icons.Outlined.AutoAwesome, "Визначити фокуси дня"),
    DAY_PLAN("План Дня", Icons.AutoMirrored.Filled.ListAlt, "Створити та керувати завданнями дня"),
    JOURNAL("Journal", Icons.AutoMirrored.Outlined.MenuBook, "Ведення щоденника"),
    FINALIZATION("Finalization", Icons.Outlined.TaskAlt, "Підбити підсумки дня"),
    DASHBOARD("Дашборд", Icons.Outlined.Timeline, "Переглянути прогрес дня"),
    ANALYTICS("Аналітика", Icons.Default.Assessment, "Статистика та аналіз продуктивності"),

    ;

    companion object {
        fun fromRouteValue(value: String?): DayManagementTab? =
            when (value?.trim()?.uppercase()) {
                "TRACK" -> JOURNAL
                "PLAN", "DAY_PLAN" -> DAY_PLAN
                "DAY_START" -> DAY_START
                "THEMES", "DAY_THEMES" -> DAY_THEMES
                "DAY_FOCUSES" -> DAY_FOCUSES
                "JOURNAL" -> JOURNAL
                "FINALIZATION" -> FINALIZATION
                "DASHBOARD" -> DASHBOARD
                "ANALYTICS" -> ANALYTICS
                else -> null
            }

        fun todaySubTabs(): List<DayManagementTab> =
            listOf(
                DAY_START,
                DAY_THEMES,
                DAY_FOCUSES,
                DAY_PLAN,
                JOURNAL,
                FINALIZATION,
            )
    }
}
