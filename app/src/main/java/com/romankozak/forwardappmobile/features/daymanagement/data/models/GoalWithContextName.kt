package com.romankozak.forwardappmobile.features.daymanagement.data.models

import com.romankozak.forwardappmobile.core.data.models.Goal

data class GoalWithContextName(
    val goal: Goal,
    val projectName: String?,
)
