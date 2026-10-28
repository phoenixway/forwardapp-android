package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.data.planning.PlanningContextCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** UI event adapter only: no copied semantic fields and no DAO writes. */
class PlanningPromotionActions @Inject constructor(private val commands: PlanningContextCoordinator) {
    fun questToTactical(scope: CoroutineScope, questId: String, message: (String) -> Unit) = run(scope, message, "Added to Tactical") {
        commands.questToTactical(questId)
    }
    fun missionToToday(scope: CoroutineScope, missionId: Long, message: (String) -> Unit) = run(scope, message, "Added to Today") {
        commands.missionToToday(missionId)
    }
    fun missionToArc(scope: CoroutineScope, missionId: Long, message: (String) -> Unit) = run(scope, message, "Planned in current Arc") {
        commands.missionToArc(missionId)
    }
    private fun run(scope: CoroutineScope, message: (String) -> Unit, success: String, action: suspend () -> Unit) = scope.launch {
        try { action(); message(success) }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { message(error.message ?: "Не вдалося додати до Planning") }
    }
}
