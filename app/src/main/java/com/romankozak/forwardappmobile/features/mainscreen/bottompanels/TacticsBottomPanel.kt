package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionViewModel
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout

@Composable
fun TacticsBottomPanel(
    onNavigateToProjectHierarchy: () -> Unit,
    onShowContextMarkersSheet: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onExportToFile: () -> Unit,
    onImportFromFileRequest: (Uri) -> Unit,
    onSelectiveImportFromFileRequest: (Uri) -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: (Uri) -> Unit,
    onWifiPush: (String) -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onShowAbout: () -> Unit,
    featureToggles: Map<FeatureFlag, Boolean>,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    val viewModel: TacticalMissionViewModel = hiltViewModel()
    val panelStyle = LocalInputPanelColors.current.addProjectLog
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }

    fun submitMission() {
        val title = inputValue.text.trim()
        if (title.isBlank()) return
        viewModel.addQuickMission(title)
        inputValue = TextFieldValue("")
    }

    CommonBottomPanelLayout {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = panelStyle.backgroundColor,
            border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = viewModel::openAddMissionDialog,
                        modifier = Modifier.size(32.dp),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Додати місію",
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = viewModel::toggleScopeLinksSheet,
                        modifier = Modifier.size(32.dp),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = "Показати зв'язки",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 3)
                                .defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = panelStyle.inputFieldColor,
                        border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.3f)),
                    ) {
                        BasicTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = panelStyle.textColor,
                                ),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitMission() }),
                            cursorBrush = SolidColor(panelStyle.textColor),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (inputValue.text.isBlank()) {
                                        Text(
                                            text = "Нова місія...",
                                            color = panelStyle.textColor.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    AnimatedVisibility(
                        visible = inputValue.text.isNotBlank(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        IconButton(
                            onClick = ::submitMission,
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .background(
                                        color = panelStyle.textColor,
                                        shape = RoundedCornerShape(22.dp),
                                    ),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    contentColor =
                                        if (panelStyle.textColor.luminance() > 0.5f) {
                                            Color.Black
                                        } else {
                                            Color.White
                                        },
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Створити місію",
                            )
                        }
                    }
                }
            }
        }
    }
}
