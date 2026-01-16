package com.romankozak.forwardappmobile.ui.dialogs

import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.AppStatistics

@Composable
fun AboutAppDialog(
    stats: AppStatistics,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo: PackageInfo? =
        remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                null
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Forward App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Total lists: ${stats.projectCount}")
                Text("Total goals: ${stats.goalCount}")
                Text("---")
                Text("Version: ${packageInfo?.versionName ?: "N/A"}")
                val versionCode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo?.longVersionCode ?: -1
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo?.versionCode?.toLong() ?: -1
                    }
                if (versionCode != -1L) {
                    Text("Build: $versionCode")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
