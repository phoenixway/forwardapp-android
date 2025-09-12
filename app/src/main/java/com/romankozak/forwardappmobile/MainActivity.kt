package com.romankozak.forwardappmobile

import ai.onnxruntime.BuildConfig
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.domain.reminders.ReminderBroadcastReceiver
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val syncDataViewModel: SyncDataViewModel by viewModels()
    private val tag = "MainActivity"

    @Inject
    lateinit var goalRepository: GoalRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Log.d(tag, "MainActivity: onCreate called")
        handleReminderIntent(intent)
        checkAndLogMissedDays()

        setContent {
            ForwardAppMobileTheme {
                Column {
                    RequestAllPermissions()
                    AppNavigation(syncDataViewModel = syncDataViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(tag, "MainActivity: onNewIntent called")
        handleReminderIntent(intent)
    }

    private fun handleReminderIntent(intent: Intent?) {
        val goalId = intent?.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID)
        if (goalId != null) {
            Log.d(tag, "MainActivity: Launched from reminder for goal ID: $goalId")

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(goalId.hashCode())

            // Show a toast to confirm the reminder was received
            Toast.makeText(this, "Reminder for goal: $goalId", Toast.LENGTH_SHORT).show()

            val cleanIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            setIntent(cleanIntent)
        }
    }

    private fun checkAndLogMissedDays() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastLogTime = prefs.getLong("last_summary_log_time", 0L)

            val lastLogCalendar = Calendar.getInstance().apply { timeInMillis = lastLogTime }
            val todayCalendar = Calendar.getInstance()

            // Якщо остання дата логування раніше, ніж сьогодні
            if (lastLogCalendar.get(Calendar.YEAR) < todayCalendar.get(Calendar.YEAR) ||
                lastLogCalendar.get(Calendar.DAY_OF_YEAR) < todayCalendar.get(Calendar.DAY_OF_YEAR)) {

                // Починаємо з дня, наступного за днем останнього логу
                val dayToProcess = lastLogCalendar.apply { add(Calendar.DAY_OF_YEAR, 1) }

                // Створюємо звіти для всіх пропущених днів до вчора включно
                while (dayToProcess.before(todayCalendar)) {
                    // Тут вам потрібно отримати ID всіх проектів, для яких треба вести лог
                    // Для прикладу, я захардкодив один ID
                    val projectId = "your_project_id_to_log" // TODO: Замініть на реальну логіку отримання ID

                    goalRepository.logProjectTimeSummaryForDate(projectId, dayToProcess)

                    dayToProcess.add(Calendar.DAY_OF_YEAR, 1)
                }

                // Зберігаємо поточний час як час останнього логування
                prefs.edit().putLong("last_summary_log_time", System.currentTimeMillis()).apply()
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestAllPermissions() {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("") }

    // Notification permission launcher
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Notification permission denied. Reminders won't work.", Toast.LENGTH_LONG).show()
            }
        }
    )

    // Exact alarm permission launcher (opens system settings)
    val alarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Exact alarm permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Exact alarm permission still denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    // Battery optimization launcher
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            Toast.makeText(context, "Battery optimization settings closed", Toast.LENGTH_SHORT).show()
        }
    )

    LaunchedEffect(Unit) {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (notificationPermission == PackageManager.PERMISSION_DENIED) {
                permissionType = "notification"
                showPermissionDialog = true
            }
        }

        // Check exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                permissionType = "alarm"
                showPermissionDialog = true
            }
        }

        // Check battery optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                Log.w("MainActivity", "App is not in battery optimization whitelist")
            }
        }
    }

    // Permission request dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    text = when(permissionType) {
                        "notification" -> "🔔 Notification Permission"
                        "alarm" -> "⏰ Exact Alarm Permission"
                        else -> "📱 Permission Required"
                    }
                )
            },
            text = {
                Text(
                    text = when(permissionType) {
                        "notification" -> "This app needs notification permission to show you goal reminders. Without this permission, you won't receive any reminders."
                        "alarm" -> "This app needs exact alarm permission to schedule precise reminders. This ensures your goals are reminded at the exact time you set."
                        else -> "This permission is required for the app to work properly."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        when(permissionType) {
                            "notification" -> {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            "alarm" -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    alarmSettingsLauncher.launch(intent)
                                }
                            }
                        }
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }

    // Debug section for testing permissions
    if (BuildConfig.DEBUG) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛠️ Debug Controls", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                permissionType = "notification"
                                showPermissionDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📱 Permissions", maxLines = 1)
                        }

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    batteryOptimizationLauncher.launch(intent)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔋 Battery", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}