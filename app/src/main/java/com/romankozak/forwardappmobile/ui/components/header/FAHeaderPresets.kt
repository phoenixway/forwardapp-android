package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

 @Composable
fun TodayHeader(onModeClick: () -> Unit): FAHeaderConfig {
    return FAHeaderConfig(
        center = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Today", style = MaterialTheme.typography.titleLarge)
                Text("Operative Mode • ${FAHeaderUtils.currentDate()}")
            }
        },
        right = {
            ModeCapsule(text = "Modes", onClick = onModeClick)
        }
    )
}

 @Composable
fun StrategyHeader(onModeClick: () -> Unit): FAHeaderConfig =
    FAHeaderConfig(
        center = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Strategy", style = MaterialTheme.typography.titleLarge)
                Text("Long-term planning mode")
            }
        },
        right = {
            ModeCapsule("Modes", onClick = onModeClick)
        }
    )

 @Composable
fun StrategicArcHeader(onModeClick: () -> Unit): FAHeaderConfig =
    FAHeaderConfig(
        center = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Strategic Arc", style = MaterialTheme.typography.titleLarge)
                Text("April • Expansion Arc")
            }
        },
        right = {
            ModeCapsule("Modes", onClick = onModeClick)
        }
    )
