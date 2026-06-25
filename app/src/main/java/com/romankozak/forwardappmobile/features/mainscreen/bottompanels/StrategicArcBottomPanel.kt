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
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatListBulleted
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
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckMoreActionButton
import com.romankozak.forwardappmobile.features.mainscreen.StrategicArcTab
import com.romankozak.forwardappmobile.features.mainscreen.StrategicArcViewModel
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout

private val PanelActionButtonSize = 48.dp
private val PanelActionIconSize = 24.dp

@Composable
@Suppress("LongParameterList", "LongMethod")
fun StrategicArcBottomPanel(
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
    viewModel: StrategicArcViewModel = hiltViewModel(),
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedInputs =
        listOf(
            onNavigateToGlobalSearch,
            onNavigateToInbox,
            onNavigateToTracker,
            onNavigateToRecentItem,
            recentViewModel,
        )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val panelStyle = LocalInputPanelColors.current.addProjectLog
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }

    fun submitQuest() {
        val title = inputValue.text.trim()
        if (title.isBlank()) return
        viewModel.addArcQuest(title)
        inputValue = TextFieldValue("")
    }

    CommonBottomPanelLayout {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = panelStyle.backgroundColor,
            border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { viewModel.selectTab(StrategicArcTab.QUESTS) },
                        modifier = Modifier.size(PanelActionButtonSize),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor =
                                    if (selectedTab == StrategicArcTab.QUESTS) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                contentColor =
                                    if (selectedTab == StrategicArcTab.QUESTS) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        panelStyle.textColor.copy(alpha = 0.8f)
                                    },
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FormatListBulleted,
                            contentDescription = "ArcQuest",
                            modifier = Modifier.size(PanelActionIconSize),
                        )
                    }
                    IconButton(
                        onClick = { viewModel.selectTab(StrategicArcTab.ARTIFACT) },
                        modifier = Modifier.size(PanelActionButtonSize),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor =
                                    if (selectedTab == StrategicArcTab.ARTIFACT) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                contentColor =
                                    if (selectedTab == StrategicArcTab.ARTIFACT) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        panelStyle.textColor.copy(alpha = 0.8f)
                                    },
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Article,
                            contentDescription = "Артефакт",
                            modifier = Modifier.size(PanelActionIconSize),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { showContextPicker = true },
                        modifier = Modifier.size(PanelActionButtonSize),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Додати контекст як ArcQuest",
                            modifier = Modifier.size(PanelActionIconSize),
                        )
                    }

                    CommandDeckMoreActionButton(
                        onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                        onShowContextMarkersSheet = onShowContextMarkersSheet,
                        onNavigateToReminders = onNavigateToReminders,
                        onNavigateToPresets = onNavigateToPresets,
                        onNavigateToAiChat = onNavigateToAiChat,
                        onNavigateToAiInsights = onNavigateToAiInsights,
                        onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
                        onNavigateToSettings = onNavigateToSettings,
                        onExportToFile = onExportToFile,
                        onImportFromFileRequest = onImportFromFileRequest,
                        onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                        onExportAttachments = onExportAttachments,
                        onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                        onWifiPush = onWifiPush,
                        onShowWifiServer = onShowWifiServer,
                        onShowWifiImport = onShowWifiImport,
                        onNavigateToAttachments = onNavigateToAttachments,
                        onNavigateToScripts = onNavigateToScripts,
                        onShowAbout = onShowAbout,
                        featureToggles = featureToggles,
                        modifier = Modifier.size(PanelActionButtonSize),
                    )
                }

                if (selectedTab == StrategicArcTab.QUESTS) {
                    Row(
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Surface(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 4)
                                    .defaultMinSize(minHeight = 44.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = panelStyle.inputFieldColor,
                            border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.3f)),
                        ) {
                            BasicTextField(
                                value = inputValue,
                                onValueChange = { inputValue = it },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = panelStyle.textColor),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { submitQuest() }),
                                cursorBrush = SolidColor(panelStyle.textColor),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (inputValue.text.isBlank()) {
                                            Text(
                                                text = "Новий ArcQuest...",
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
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { inputValue = TextFieldValue("") },
                                    modifier = Modifier.size(PanelActionButtonSize),
                                    colors =
                                        IconButtonDefaults.iconButtonColors(
                                            contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Очистити ArcQuest",
                                        modifier = Modifier.size(PanelActionIconSize),
                                    )
                                }
                                IconButton(
                                    onClick = ::submitQuest,
                                    modifier =
                                        Modifier
                                            .size(PanelActionButtonSize)
                                            .background(panelStyle.textColor, RoundedCornerShape(24.dp)),
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
                                        contentDescription = "Створити ArcQuest",
                                        modifier = Modifier.size(PanelActionIconSize),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContextPicker) {
        LinkedTargetsPickerDialog(
            contextOptions =
                uiState.allProjects.map {
                    ProjectOption(id = it.id, name = it.name, parentId = it.parentId)
                },
            attachmentOptions = emptyList(),
            preselectedContextIds = uiState.arcQuests.mapNotNull { it.linkedContextId }.toSet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.CONTEXTS,
            allowedTabs = setOf(LinkPickerTab.CONTEXTS),
            onDismiss = { showContextPicker = false },
            onContextSelected = { contextId ->
                viewModel.addArcQuestFromContext(contextId)
                showContextPicker = false
            },
            onAttachmentSelected = {},
            onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
            onCreateDocument = null,
        )
    }
}
