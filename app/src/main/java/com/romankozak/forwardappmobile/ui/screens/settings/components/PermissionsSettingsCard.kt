package com.romankozak.forwardappmobile.ui.screens.settings.components

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionsSettingsCard() {
    val context = LocalContext.current
    var permissionUpdateTrigger by remember { mutableIntStateOf(0) }

    val hasNotificationPermission = remember(permissionUpdateTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val canScheduleExactAlarms = remember(permissionUpdateTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { permissionUpdateTrigger++ }
    )

    SettingsCard(title = "Permissions", icon = Icons.Default.Security) {
        PermissionRow(
            icon = Icons.Default.Notifications,
            name = "Notifications",
            description = "Required to show reminders",
            isGranted = hasNotificationPermission,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        PermissionRow(
            icon = Icons.Default.Alarm,
            name = "Exact Alarms",
            description = "Required for reminders to be on time",
            isGranted = canScheduleExactAlarms,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionRow(
    icon: ImageVector,
    name: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(name) },
        supportingContent = { Text(description) },
        trailingContent = {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF388E3C)
                )
            } else {
                Button(onClick = onGrantClick, contentPadding = PaddingValues(horizontal = 16.dp)) {
                    Text("Grant")
                }
            }
        }
    )
}
