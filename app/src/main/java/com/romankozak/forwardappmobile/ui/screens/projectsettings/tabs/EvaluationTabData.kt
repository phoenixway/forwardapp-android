package com.romankozak.forwardappmobile.ui.screens.projectsettings.tabs

data class EvaluationTabUiState(
    val valueImportance: Float,
    val valueImpact: Float,
    val effort: Float,
    val cost: Float,
    val risk: Float,
    val weightEffort: Float,
    val weightCost: Float,
    val weightRisk: Float,
    val rawScore: Float,
    val scoringStatus: String,
    val isScoringEnabled: Boolean,
)

interface EvaluationTabActions {
    fun onValueImportanceChange(value: Float)
    fun onValueImpactChange(value: Float)
    fun onEffortChange(value: Float)
    fun onCostChange(value: Float)
    fun onRiskChange(value: Float)
    fun onWeightEffortChange(value: Float)
    fun onWeightCostChange(value: Float)
    fun onWeightRiskChange(value: Float)
    fun onScoringStatusChange(newStatus: String)
}