package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.mainscreen.StrategicArcTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualAction
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualActionsButton
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualComposerSurface
@Composable
internal fun StrategicArcBottomPanelContent(
    selectedTab: StrategicArcTab,
    inputValue: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSubmitQuest: () -> Unit,
    onSelectTab: (StrategicArcTab) -> Unit,
    onShowContextPicker: () -> Unit,
    onClearInput: () -> Unit,
    additionalActions: List<ContextualAction> = emptyList(),
) {
    val actions = strategicComposerActions(selectedTab, inputValue.text.isNotBlank(), onSelectTab, onShowContextPicker, onClearInput) + additionalActions
    ContextualComposerSurface {
        if (selectedTab == StrategicArcTab.QUESTS) {
            BottomPanelComposer(
                inputValue = inputValue, onValueChange = onInputChange, onSubmit = onSubmitQuest,
                placeholderText = "Новий ArcQuest...", maxHeightScreenFraction = 4,
                sendContentDescription = "Створити ArcQuest", compact = true,
                trailingContent = {
                    ContextualActionsButton(
                        "Стратегічна арка · Квести", actions,
                    )
                },
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Артефакт арки", Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium)
                ContextualActionsButton(
                    "Стратегічна арка · Артефакт", actions,
                )
            }
        }
    }
}

internal fun strategicComposerActions(
    selectedTab: StrategicArcTab,
    hasDraft: Boolean,
    onSelectTab: (StrategicArcTab) -> Unit,
    onShowContextPicker: () -> Unit,
    onClearInput: () -> Unit,
) = listOf(
    ContextualAction(
        "quest-source", "Створити Quest на основі сутності", Icons.Default.Add, onClick = onShowContextPicker,
    ),
    ContextualAction(
        "quest-clear", "Очистити ArcQuest", Icons.Outlined.Close, enabled = hasDraft, onClick = onClearInput,
    ),
    ContextualAction(
        "quests", "Квести арки", Icons.AutoMirrored.Outlined.FormatListBulleted,
        selected = selectedTab == StrategicArcTab.QUESTS, onClick = { onSelectTab(StrategicArcTab.QUESTS) },
    ),
    ContextualAction(
        "artifact", "Артефакт", Icons.AutoMirrored.Outlined.Article,
        selected = selectedTab == StrategicArcTab.ARTIFACT, onClick = { onSelectTab(StrategicArcTab.ARTIFACT) },
    ),
)
