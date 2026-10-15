package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ParameterSlider(
    state: ParameterSliderConfig,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
) {
    val effectiveScale =
        state.unsetValue
            ?.takeIf { it !in state.scale }
            ?.let { listOf(it) + state.scale }
            ?: state.scale

    val currentIndex = effectiveScale.indexOf(state.value).coerceAtLeast(0)
    val isUnset =
        state.unsetValue != null &&
            effectiveScale.getOrNull(currentIndex) == state.unsetValue

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.label, style = MaterialTheme.typography.bodyLarge)
            val displayText =
                when {
                    isUnset -> state.unsetLabel
                    state.valueLabels != null -> {
                        val labelIndex =
                            if (state.unsetValue != null && state.unsetValue !in state.scale) {
                                currentIndex - 1
                            } else {
                                currentIndex
                            }
                        state.valueLabels.getOrElse(labelIndex) { state.value.toString() }
                    }
                    state.scale == Scales.weights -> "x${"%.1f".format(state.value)}"
                    else -> state.value.toInt().toString()
                }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            enabled = enabled,
            value = currentIndex.toFloat(),
            onValueChange = { newIndex ->
                val roundedIndex = newIndex.roundToInt().coerceIn(0, effectiveScale.lastIndex)
                onValueChange(effectiveScale[roundedIndex])
            },
            valueRange = 0f..effectiveScale.lastIndex.toFloat(),
            steps = (effectiveScale.size - 2).coerceAtLeast(0),
        )
    }
}
