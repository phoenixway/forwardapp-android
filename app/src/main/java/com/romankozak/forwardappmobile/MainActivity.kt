package com.romankozak.forwardappmobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val syncDataViewModel: SyncDataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ForwardAppMobileTheme {
                // --- ПОЧАТОК ЗМІНИ: Запит дозволу на сповіщення ---
                RequestNotificationPermission()
                // --- КІНЕЦЬ ЗМІНИ ---
                AppNavigation(syncDataViewModel = syncDataViewModel)
            }
        }
    }
}

// --- ПОЧАТОК ЗМІНИ: Нова функція для запиту дозволу ---
@Composable
private fun RequestNotificationPermission() {
    // Працює тільки на Android 13 (API 33) і вище
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    // Дозвіл надано, все добре
                } else {
                    // Користувач відхилив дозвіл.
                    // Можна показати повідомлення з поясненням, чому дозвіл важливий.
                }
            }
        )

        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
// --- КІНЕЦЬ ЗМІНИ ---