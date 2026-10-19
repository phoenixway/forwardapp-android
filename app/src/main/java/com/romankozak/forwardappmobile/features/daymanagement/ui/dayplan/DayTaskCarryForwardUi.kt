package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import android.util.Log
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import kotlinx.coroutines.CancellationException

/** Commit visibility/confirmation is independent from post-commit metric calculation. */
internal suspend fun carryDayTaskToTomorrow(
    repository: DayManagementRepository,
    task: DayTaskWithReminder,
    onCommitted: () -> Unit,
    message: (String) -> Unit,
) {
    try {
        val carried = repository.moveTaskToTomorrow(task.dayTask)
        onCommitted()
        message("Додано на наступний день. Участь поточного дня збережено.")
        try {
            repository.calculateAndSaveDailyMetrics(carried.dayPlanId)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) { Log.e("DayCarryForward", "Post-commit metrics failed", failure) }
    } catch (cancelled: CancellationException) { throw cancelled
    } catch (failure: Exception) {
        message("Не вдалося додати на наступний день: ${failure.message.orEmpty()}")
    }
}
