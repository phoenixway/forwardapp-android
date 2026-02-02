package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import com.romankozak.forwardappmobile.core.data.models.entities.Goal

data class GoalWithContextName(
    val goal: Goal,
    val projectName: String?,
)
