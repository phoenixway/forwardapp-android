package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.theme.ThemeName
import com.romankozak.forwardappmobile.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ThemingUseCase @Inject constructor(
    private val settingsRepo: SettingsRepository,
) {
    val themeSettings: StateFlow<com.romankozak.forwardappmobile.ui.theme.ThemeSettings> = settingsRepo.themeSettings

    fun updateLightTheme(scope: CoroutineScope, themeName: ThemeName) {
        scope.launch {
            settingsRepo.saveThemeSettings(
                themeSettings.value.copy(lightThemeName = themeName)
            )
        }
    }

    fun updateDarkTheme(scope: CoroutineScope, themeName: ThemeName) {
        scope.launch {
            settingsRepo.saveThemeSettings(
                themeSettings.value.copy(darkThemeName = themeName)
            )
        }
    }

    fun updateThemeMode(scope: CoroutineScope, themeMode: ThemeMode) {
        scope.launch {
            settingsRepo.saveThemeSettings(
                themeSettings.value.copy(themeMode = themeMode)
            )
        }
    }
}
