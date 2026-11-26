package com.romankozak.forwardappmobile.features.sync

internal fun wifiSyncErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty()
    return if (message.contains("malformed JSON", ignoreCase = true) ||
        message.contains("JsonReader", ignoreCase = true)) {
        "Інший пристрій повернув некоректні дані. Перегляньте журнал синхронізації на ньому."
    } else if (message.contains("connect timeout", ignoreCase = true) ||
        message.contains("connection refused", ignoreCase = true) ||
        message.contains("failed to connect", ignoreCase = true)) {
        "Не вдалося підключитися. Перевірте Wi‑Fi, адресу IP:порт і чи дозволено підключення на іншому пристрої."
    } else message.ifBlank { "Не вдалося синхронізуватися" }
}
