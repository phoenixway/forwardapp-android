package com.romankozak.forwardappmobile.shared.data.models

object ProjectStatusValues {
    const val NO_PLAN = "NO_PLAN"
    const val PLANNING = "PLANNING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val ON_HOLD = "ON_HOLD"
    const val PAUSED = "PAUSED"

    fun getDisplayName(status: String?): String {
        return when (status) {
            NO_PLAN -> "Без плану"
            PLANNING -> "Планується"
            IN_PROGRESS -> "В реалізації"
            COMPLETED -> "Завершено"
            ON_HOLD -> "Відкладено"
            PAUSED -> "На паузі"
            else -> "Без плану"
        }
    }
}

/*object ScoringStatusValues {
    const val NOT_ASSESSED = "NOT_ASSESSED"
    const val IMPOSSIBLE_TO_ASSESS = "IMPOSSIBLE_TO_ASSESS"
    const val ASSESSED = "ASSESSED"
}*/

object ProjectLogLevelValues {
    const val DETAILED = "DETAILED"
    const val NORMAL = "NORMAL"
}
