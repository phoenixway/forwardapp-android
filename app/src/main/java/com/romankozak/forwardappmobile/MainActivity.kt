package com.romankozak.forwardappmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val syncDataViewModel: SyncDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- ПОЧАТОК ЗМІН ---
        // Вмикаємо режим "edge-to-edge", щоб UI малювався під системними панелями
        enableEdgeToEdge()
        // --- КІНЕЦЬ ЗМІН ---

        super.onCreate(savedInstanceState)

        // Цей рядок більше не потрібен, оскільки enableEdgeToEdge() робить те саме
        // WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ForwardAppMobileTheme {
                AppNavigation(syncDataViewModel = syncDataViewModel)
            }
        }
    }
}