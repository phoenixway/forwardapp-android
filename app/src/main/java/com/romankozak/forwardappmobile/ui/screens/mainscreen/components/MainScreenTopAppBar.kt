package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.NorthEast
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenTopAppBar(
    isSearchActive: Boolean,
    isFocusMode: Boolean,
    focusedProjectTitle: String?,
    focusedProjectMenuClick: (() -> Unit)?,
    focusedProjectOpenClick: (() -> Unit)?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onShowHistory: () -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onShowImportExportSheet: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    onShowReminders: () -> Unit,
    onShowAttachmentsLibrary: () -> Unit,
    onShowScriptsLibrary: () -> Unit,
) {
    var swipeState by remember { mutableStateOf(0f) }
    TopAppBar(
        title = {
            if (isFocusMode && focusedProjectTitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = focusedProjectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .let { base ->
                                    if (focusedProjectOpenClick != null) {
                                        base.clickable(onClick = focusedProjectOpenClick)
                                    } else {
                                        base
                                    }
                                },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (focusedProjectOpenClick != null) {
                        IconButton(
                            onClick = focusedProjectOpenClick,
                            modifier = Modifier.size(36.dp).padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NorthEast,
                                contentDescription = "Open project",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeonTitle(stringResource(id = com.romankozak.forwardappmobile.R.string.app_name))
                    if (com.romankozak.forwardappmobile.BuildConfig.DEBUG || com.romankozak.forwardappmobile.BuildConfig.IS_EXPERIMENTAL_BUILD) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Text(
                                // Show "Debug" if DEBUG build, otherwise "Experimental" for exp release
                                text = if (com.romankozak.forwardappmobile.BuildConfig.DEBUG) "Debug" else "Experimental",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                if (isFocusMode && focusedProjectMenuClick != null) {
                    IconButton(onClick = focusedProjectMenuClick) {
                        Icon(Icons.Default.MoreVert, stringResource(id = com.romankozak.forwardappmobile.R.string.more_options))
                    }
                } else {
                    AnimatedVisibility(visible = false) {
                        IconButton(onClick = onGoBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(id = com.romankozak.forwardappmobile.R.string.back))
                        }
                    }
                    AnimatedVisibility(visible = false) {
                        IconButton(onClick = onGoForward) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(id = com.romankozak.forwardappmobile.R.string.forward))
                        }
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(id = com.romankozak.forwardappmobile.R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        val showAttachmentsLibrary = FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary)
                        val showScriptsLibrary = FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary)
                        if (showAttachmentsLibrary || showScriptsLibrary) {
                            if (showAttachmentsLibrary) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = com.romankozak.forwardappmobile.R.string.menu_attachments_library)) },
                                    onClick = {
                                        onShowAttachmentsLibrary()
                                        menuExpanded = false
                                    },
                                )
                            }
                            if (showScriptsLibrary) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = com.romankozak.forwardappmobile.R.string.menu_scripts_library)) },
                                    onClick = {
                                        onShowScriptsLibrary()
                                        menuExpanded = false
                                    },
                                )
                            }
                            HorizontalDivider()
                        }
                        if (FeatureToggles.isEnabled(FeatureFlag.WifiSync)) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = com.romankozak.forwardappmobile.R.string.menu_run_wifi_server)) },
                                onClick = {
                                    onShowWifiServer()
                                    menuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = com.romankozak.forwardappmobile.R.string.menu_import_wifi)) },
                                onClick = {
                                    onShowWifiImport()
                                    menuExpanded = false
                                },
                            )
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(id = com.romankozak.forwardappmobile.R.string.menu_import_export)) },
                            onClick = {
                                menuExpanded = false
                                onShowImportExportSheet()
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
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier =
            Modifier.pointerInput(Unit) {
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
