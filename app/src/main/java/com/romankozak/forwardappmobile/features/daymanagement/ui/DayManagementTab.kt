package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

enum class DayManagementTab(val title: String, val icon: ImageVector, val description: String) {
    TRACK("Трекер", Icons.Outlined.Timeline, "Відстежувати активність"),
    DAY_START("День Початок", Icons.Default.Dashboard, "Започаткувати день"),
    DAY_FOCUSES("День Фокуси", Icons.Default.Dashboard, "Визначити фокуси дня"),
    DAY_PLAN("План Дня", Icons.AutoMirrored.Filled.ListAlt, "Створити та керувати завданнями дня"),
    JOURNAL("Журнал", Icons.Default.Dashboard, "Ведення щоденника"),
    FINALIZATION("Завершення", Icons.Default.Dashboard, "Підбити підсумки дня"),
    DASHBOARD("Дашборд", Icons.Default.Dashboard, "Переглянути прогрес дня"),
    ANALYTICS("Аналітика", Icons.Default.Assessment, "Статистика та аналіз продуктивності"),
}
