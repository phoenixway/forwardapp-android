package com.romankozak.forwardappmobile.domain.reminders

/**
 * User-facing ringtone intensity presets for reminders.
 */
enum class RingtoneType(val storageKey: String, val title: String) {
    Energetic("energetic", "Енергійний"),
    Moderate("moderate", "Помірний"),
    Quiet("quiet", "Тихий");

    companion object {
        fun fromStorage(value: String?): RingtoneType =
            values().firstOrNull { it.storageKey == value } ?: Energetic
    }
}
