package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.theme.InputModeColors
import com.romankozak.forwardappmobile.features.mainscreen.StrategicArcTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelActionRow
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalRail
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelIconButton
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelSurface
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelToggleButton

@Composable
internal fun StrategicArcBottomPanelContent(
    selectedTab: StrategicArcTab,
    inputValue: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSubmitQuest: () -> Unit,
    onSelectTab: (StrategicArcTab) -> Unit,
    onShowContextPicker: () -> Unit,
    onClearInput: () -> Unit,
    globalActions: BottomPanelGlobalActions,
    panelStyle: InputModeColors,
) {
    BottomPanelSurface(panelStyle = panelStyle) {
        BottomPanelActionRow(
            leadingContent = {
                BottomPanelToggleButton(
                    imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    contentDescription = "ArcQuest",
                    selected = selectedTab == StrategicArcTab.QUESTS,
                    panelStyle = panelStyle,
                    onClick = { onSelectTab(StrategicArcTab.QUESTS) },
                )
                BottomPanelToggleButton(
                    imageVector = Icons.AutoMirrored.Outlined.Article,
                    contentDescription = "Артефакт",
                    selected = selectedTab == StrategicArcTab.ARTIFACT,
                    panelStyle = panelStyle,
                    onClick = { onSelectTab(StrategicArcTab.ARTIFACT) },
                )
            },
            trailingContent = {
                BottomPanelIconButton(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Додати контекст як ArcQuest",
                    panelStyle = panelStyle,
                    onClick = onShowContextPicker,
                )
                BottomPanelGlobalRail(
                    actions = globalActions,
                    panelStyle = panelStyle,
                )
            },
        )

        if (selectedTab == StrategicArcTab.QUESTS) {
            BottomPanelComposer(
                inputValue = inputValue,
                onValueChange = onInputChange,
                onSubmit = onSubmitQuest,
                panelStyle = panelStyle,
                placeholderText = "Новий ArcQuest...",
                maxHeightScreenFraction = 4,
                sendContentDescription = "Створити ArcQuest",
                secondarySubmitActions = {
                    BottomPanelIconButton(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Очистити ArcQuest",
                        panelStyle = panelStyle,
                        onClick = onClearInput,
                    )
                },
            )
        }
    }
}
