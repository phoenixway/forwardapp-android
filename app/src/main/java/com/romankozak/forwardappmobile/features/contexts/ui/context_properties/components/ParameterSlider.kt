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
    val currentIndex = state.scale.indexOf(state.value).coerceAtLeast(0)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.label, style = MaterialTheme.typography.bodyLarge)
            val displayText =
                when {
                    state.valueLabels != null -> state.valueLabels.getOrElse(currentIndex) { state.value.toString() }
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
                val roundedIndex = newIndex.roundToInt().coerceIn(0, state.scale.lastIndex)
                onValueChange(state.scale[roundedIndex])
            },
            valueRange = 0f..state.scale.lastIndex.toFloat(),
            steps = (state.scale.size - 2).coerceAtLeast(0),
        )
    }
}
