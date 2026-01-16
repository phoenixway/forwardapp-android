package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.components.projectrealization
import androidx.compose.runtime.Composable

@Composable
fun InsightsContent(isManagementEnabled: Boolean) {
    if (!isManagementEnabled) {
        PlaceholderContent(text = "Увімкніть підтримку реалізації на Дашборді, щоб бачити інсайти.")
        return
    }
    PlaceholderContent(text = "Інсайти та аналітика по проекту (в розробці).")
}
