package com.romankozak.forwardappmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.romankozak.forwardappmobile.routes.AppNavigation
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
class MainActivity : ComponentActivity() {
    // private val syncDataViewModel: SyncDataViewModel by viewModels() // Removed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val appComponent = (application as ForwardAppMobileApplication).appComponent
            androidx.compose.runtime.CompositionLocalProvider(com.romankozak.forwardappmobile.di.LocalAppComponent provides appComponent) {
                ForwardAppMobileTheme(themeSettings = com.romankozak.forwardappmobile.ui.theme.ThemeSettings()) {
                    AppNavigation()
                }
            }
        }
    }
}
