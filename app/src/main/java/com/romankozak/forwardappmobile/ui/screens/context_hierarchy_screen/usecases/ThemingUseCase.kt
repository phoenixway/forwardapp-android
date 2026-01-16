package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.theme.ThemeName
import com.romankozak.forwardappmobile.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class ThemingUseCase @Inject constructor(
    private val settingsRepo: SettingsRepository,
) {
    val themeSettings: Flow<com.romankozak.forwardappmobile.ui.theme.ThemeSettings> = settingsRepo.themeSettings

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
