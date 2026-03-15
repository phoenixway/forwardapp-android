package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.components

data class ParameterSliderConfig(
    val label: String,
    val value: Float,
    val scale: List<Float>,
    val valueLabels: List<String>? = null,
)
