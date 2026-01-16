package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.theme.ThemeName
import com.romankozak.forwardappmobile.ui.theme.ThemeMode
import com.romankozak.forwardappmobile.ui.theme.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class ThemingUseCase @Inject constructor(
    private val settingsRepo: SettingsRepository,
) {
    val themeSettings: Flow<ThemeSettings> = settingsRepo.themeSettings

    fun updateLightTheme(scope: CoroutineScope, themeName: ThemeName) {
        scope.launch {
            val current = settingsRepo.themeSettings.first()
            settingsRepo.saveThemeSettings(
                current.copy(lightThemeName = themeName)
            )
        }
    }

    fun updateDarkTheme(scope: CoroutineScope, themeName: ThemeName) {
        scope.launch {
            val current = settingsRepo.themeSettings.first()
            settingsRepo.saveThemeSettings(
                current.copy(darkThemeName = themeName)
            )
        }
    }

    fun updateThemeMode(scope: CoroutineScope, themeMode: ThemeMode) {
        scope.launch {
            val current = settingsRepo.themeSettings.first()
            settingsRepo.saveThemeSettings(
                current.copy(themeMode = themeMode)
            )
        }
    }
}
