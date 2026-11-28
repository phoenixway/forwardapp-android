package com.romankozak.forwardappmobile.features.sync

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
internal fun syncStatusColor(outcome: String): Color = when (outcome) {
    "success" -> if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF8CC9A5) else Color(0xFF287653)
    "error" -> MaterialTheme.colorScheme.error
    "running" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun SyncStatusIcon(outcome: String, contentDescription: String?, modifier: Modifier = Modifier) {
    val image = when (outcome) {
        "success" -> Icons.Outlined.CheckCircleOutline
        "error" -> Icons.Outlined.ErrorOutline
        "running" -> Icons.Outlined.Sync
        else -> Icons.Outlined.RemoveCircleOutline
    }
    Icon(image, contentDescription, modifier, tint = syncStatusColor(outcome))
}

internal fun syncEventTitle(event: WifiSyncEvent): String = when (event.outcome) {
    "success" -> when (event.mode) {
        "Вхідний експорт" -> "Дані передано"
        "Вхідний імпорт" -> "Дані прийнято"
        "Надсилання" -> "Дані надіслано"
        "Імпорт" -> "Дані отримано"
        else -> "Синхронізовано"
    }
    "error" -> when (event.stage) {
        "Підготовка даних" -> "Помилка підготовки даних"
        "Надсилання даних" -> "Помилка надсилання"
        "Отримання даних" -> "Помилка отримання"
        "Злиття даних" -> "Помилка злиття"
        "Підключення" -> "Помилка підключення"
        "Підготовка даних" -> "Помилка підготовки даних"
        else -> "Помилка · ${event.stage}"
    }
    "cancelled" -> "Синхронізацію перервано"
    else -> "Синхронізація триває"
}
