package com.romankozak.forwardappmobile

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
                RequestNotificationPermission()
                AppNavigation(syncDataViewModel = syncDataViewModel)
            }
        }
    }
}

@Composable
private fun RequestNotificationPermission() {
    // Працює тільки на Android 13 (API 33) і вище
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // --- ЗМІНЕНО: Додано отримання контексту ---
        val context = LocalContext.current

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

        // --- ЗМІНЕНО: Запускаємо запит, тільки якщо дозвіл ще не надано ---
        LaunchedEffect(Unit) {
            val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionStatus == PackageManager.PERMISSION_DENIED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}