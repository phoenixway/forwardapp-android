package com.romankozak.forwardappmobile.core.data.models.day_management

import com.romankozak.forwardappmobile.core.data.models.Goal

data class GoalWithContextName(
    val goal: Goal,
    val projectName: String?,
)
