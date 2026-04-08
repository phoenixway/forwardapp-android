package com.romankozak.forwardappmobile.features.mainscreen.session

import com.romankozak.forwardappmobile.core.context.SystemContexts

enum class SessionMode(
    val title: String,
    val shortDescription: String,
    val focusPrompt: String,
    val systemContextId: String?,
) {
    IMPROVE(
        title = "IMPROVE",
        shortDescription = "Уточнення планів, цілей і орієнтирів.",
        focusPrompt = "Уточни напрям, критерії і найближчі рішення.",
        systemContextId = SystemContexts.SESSION_IMPROVE.raw,
    ),
    EXECUTION(
        title = "EXECUTION",
        shortDescription = "Операційне виконання поточного дня або блоку роботи.",
        focusPrompt = "Тримай темп, обмеж потік і добивай поточні дії.",
        systemContextId = SystemContexts.SESSION_EXECUTION.raw,
    ),
    CONTROL(
        title = "CONTROL",
        shortDescription = "Огляд, корекція і втручання, коли щось не працює.",
        focusPrompt = "Знайди відхилення, звузь проблему і наведи керування.",
        systemContextId = SystemContexts.SESSION_CONTROL.raw,
    ),
    RECOVERY(
        title = "RECOVERY",
        shortDescription = "Відновлення контексту, ритму і нормального режиму роботи.",
        focusPrompt = "Поверни базовий порядок, енергію і ясність.",
        systemContextId = SystemContexts.SESSION_RECOVERY.raw,
    ),
    EMERGENCY(
        title = "EMERGENCY",
        shortDescription = "Кризовий режим для втрат керованості або різких збоїв.",
        focusPrompt = "Стабілізуй ситуацію, прибери зайве і зафіксуй наступний крок.",
        systemContextId = SystemContexts.SESSION_EMERGENCY.raw,
    ),
    UNSET(
        title = "UNSET",
        shortDescription = "Режим сесії вимкнено.",
        focusPrompt = "Оберіть режим, щоб зібрати головний екран під поточну задачу.",
        systemContextId = null,
    ),
    ;

    companion object {
        fun fromStorage(value: String?): SessionMode = entries.firstOrNull { it.name == value } ?: UNSET
    }
}

data class SessionModeState(
    val mode: SessionMode = SessionMode.UNSET,
    val startedAt: Long? = null,
) {
    val isActive: Boolean = mode != SessionMode.UNSET && startedAt != null
}

data class SessionModeChangeResult(
    val previousMode: SessionMode?,
    val newMode: SessionMode,
)
