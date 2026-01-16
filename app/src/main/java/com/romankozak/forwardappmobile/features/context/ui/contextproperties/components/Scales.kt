package com.romankozak.forwardappmobile.features.context.ui.contextproperties.components

object Scales {
    val effort = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val importance = (1..12).map { it.toFloat() }
    val impact = listOf(1f, 2f, 3f, 5f, 8f, 13f)
    val cost = (0..5).map { it.toFloat() }
    val risk = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val weights = (0..20).map { it * 0.1f }
    val costLabels = listOf("немає", "дуже низькі", "низькі", "середні", "високі", "дуже високі")
}
