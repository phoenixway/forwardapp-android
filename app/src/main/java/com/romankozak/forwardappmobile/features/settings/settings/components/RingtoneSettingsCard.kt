package com.romankozak.forwardappmobile.features.settings.settings.components

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.domain.reminders.RingtoneType

@Composable
fun RingtoneSettingsCard(
    currentType: RingtoneType,
    ringtoneUris: Map<RingtoneType, String>,
    ringtoneVolumes: Map<RingtoneType, Float>,
    vibrationEnabled: Boolean,
    onTypeSelected: (RingtoneType) -> Unit,
    onRingtonePicked: (RingtoneType, String) -> Unit,
    onVolumeChanged: (RingtoneType, Float) -> Unit,
    onVibrationToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            previewPlayer = null
        }
    }

    SettingsCard(
        title = "Рінгтон нагадувань",
        icon = Icons.Default.NotificationsActive,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Вібрація нагадувань", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Застосовується до всіх типів нагадувань",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = vibrationEnabled, onCheckedChange = onVibrationToggle)
            }

            Text("Тип гучності", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RingtoneType.values().forEach { type ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = currentType == type,
                            onClick = { onTypeSelected(type) },
                        )
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(type.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = when (type) {
                                    RingtoneType.Energetic -> "Гучний, енергійний сигнал"
                                    RingtoneType.Moderate -> "Збалансований сигнал"
                                    RingtoneType.Quiet -> "М’який, стриманий сигнал"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Налаштування рінгтонів", style = MaterialTheme.typography.titleMedium)

            RingtoneType.values().forEach { type ->
                val currentUri = ringtoneUris[type].orEmpty()
                var lastPicked by remember(type, currentUri) { mutableStateOf(currentUri) }
                var volume by remember(type, ringtoneVolumes[type]) { mutableStateOf(ringtoneVolumes[type] ?: 1f) }

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                    uri?.let {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                it,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        } catch (_: SecurityException) {
                            // Some providers do not support persistable permissions; ignore.
                        }
                        lastPicked = it.toString()
                        onRingtonePicked(type, it.toString())
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(type.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = ringtoneLabel(context, type, lastPicked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                onVolumeChanged(type, it)
                            },
                            valueRange = 0f..1f,
                            steps = 0,
                            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = "Гучність: ${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type.toSystemType())
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, lastPicked.takeIf { it.isNotBlank() }?.let(Uri::parse))
                            }
                            launcher.launch(intent)
                        }) {
                            Text(if (lastPicked.isBlank()) "Обрати" else "Змінити")
                        }
                        Button(onClick = {
                            previewPlayer?.let {
                                it.stop()
                                it.release()
                            }
                            previewPlayer = playPreview(
                                context = context,
                                uriString = lastPicked,
                                type = type,
                                volume = volume,
                            )
                        }) {
                            Text("Тест")
                        }
                    }
                }
            }
        }
    }
}

private fun ringtoneLabel(context: Context, type: RingtoneType, uriString: String): String {
    if (uriString.isBlank()) return "Системний за замовчуванням"
    return try {
        val title = RingtoneManager.getRingtone(context, Uri.parse(uriString))?.getTitle(context)
        title ?: uriString
    } catch (e: SecurityException) {
        uriString
    } catch (e: Exception) {
        uriString
    }
}

private fun RingtoneType.toSystemType(): Int =
    when (this) {
        RingtoneType.Energetic -> RingtoneManager.TYPE_ALARM
        RingtoneType.Moderate -> RingtoneManager.TYPE_RINGTONE
        RingtoneType.Quiet -> RingtoneManager.TYPE_NOTIFICATION
    }

private fun playPreview(
    context: Context,
    uriString: String,
    type: RingtoneType,
    volume: Float,
): MediaPlayer? {
    val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse)
        ?: defaultUriFor(type)
        ?: return null

    return try {
        MediaPlayer().apply {
            setDataSource(context, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = false
            setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
            setOnCompletionListener {
                it.release()
            }
            prepare()
            start()
        }
    } catch (_: Exception) {
        null
    }
}

private fun defaultUriFor(type: RingtoneType): Uri? =
    when (type) {
        RingtoneType.Energetic -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        RingtoneType.Moderate -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        RingtoneType.Quiet -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
