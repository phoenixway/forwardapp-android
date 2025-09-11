package com.romankozak.forwardappmobile.ui.screens.backlog.components.projectrealization
import androidx.compose.runtime.Composable

@Composable
fun InsightsContent(isManagementEnabled: Boolean) {
    if (!isManagementEnabled) {
        PlaceholderContent(text = "Увімкніть підтримку реалізації на Дашборді, щоб бачити інсайти.")
        return
    }
    PlaceholderContent(text = "Інсайти та аналітика по проекту (в розробці).")
}