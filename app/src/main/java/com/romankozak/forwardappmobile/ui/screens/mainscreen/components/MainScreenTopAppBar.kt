package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.config.FeatureToggles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenTopAppBar(
    isSearchActive: Boolean,
    isFocusMode: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onShowHistory: () -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onExportToFile: () -> Unit,
    onImportFromFile: () -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachments: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    onShowReminders: () -> Unit,
    onShowAttachmentsLibrary: () -> Unit,
) {
    var swipeState by remember { mutableStateOf(0f) }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeonTitle("Projects")
                if (com.romankozak.forwardappmobile.BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Text(
                            text = "Debug",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                AnimatedVisibility(visible = false) {
                    IconButton(onClick = onGoBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад")
                    }
                }
                AnimatedVisibility(visible = false) {
                    IconButton(onClick = onGoForward) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Вперед")
                    }
                }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "Menu")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    if (FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary)) {
                        DropdownMenuItem(
                            text = { Text("Attachments library") },
                            onClick = {
                                onShowAttachmentsLibrary()
                                menuExpanded = false
                            },
                        )
                        HorizontalDivider()
                    }
                    if (FeatureToggles.isEnabled(FeatureFlag.WifiSync)) {
                        DropdownMenuItem(
                            text = { Text("Run Wi-Fi Server") },
                            onClick = {
                                onShowWifiServer()
                                menuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Import from Wi-Fi") },
                            onClick = {
                                onShowWifiImport()
                                menuExpanded = false
                            },
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("Export to file") },
                        onClick = {
                            onExportToFile()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Import from file") },
                        onClick = {
                            onImportFromFile()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Export Attachments (JSON)") },
                        onClick = {
                            onExportAttachments()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Import Attachments (JSON)") },
                        onClick = {
                            onImportAttachments()
                            menuExpanded = false
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            onShowSettings()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = {
                            onShowAbout()
                            menuExpanded = false
                        },
                    )

                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = Modifier
            .shadow(4.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeState += dragAmount
                    },
                    onDragEnd = {
                        if (swipeState > 50) {
                            onGoBack()
                        } else if (swipeState < -50) {
                            onGoForward()
                        }
                        swipeState = 0f
                    }
                )
            }
    )
}

@Composable
private fun NeonTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.primary

    Box(
        modifier =
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
            MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = textColor,
        )
    }
}
