package com.romankozak.forwardappmobile.features.settings.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.theme.ThemeMode
import com.romankozak.forwardappmobile.ui.theme.ThemeName
import com.romankozak.forwardappmobile.ui.theme.ThemeSettings

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsCard(
    themeSettings: ThemeSettings,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onLightThemeSelected: (ThemeName) -> Unit,
    onDarkThemeSelected: (ThemeName) -> Unit
) {
    SettingsCard(
        title = "Theme",
        icon = Icons.Default.InvertColors,
    ) {
        Column {
            Text("Theme Mode")
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.values().forEach { mode ->
                    FilterChip(
                        selected = themeSettings.themeMode == mode,
                        onClick = { onThemeModeSelected(mode) },
                        label = { Text(mode.name) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Light Theme")
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ThemeName.values().forEach { theme ->
                    FilterChip(
                        selected = themeSettings.lightThemeName == theme,
                        onClick = { onLightThemeSelected(theme) },
                        label = { Text(theme.displayName) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Dark Theme")
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ThemeName.values().forEach { theme ->
                    FilterChip(
                        selected = themeSettings.darkThemeName == theme,
                        onClick = { onDarkThemeSelected(theme) },
                        label = { Text(theme.displayName) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}
