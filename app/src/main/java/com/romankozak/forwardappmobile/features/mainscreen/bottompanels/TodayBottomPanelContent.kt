package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.features.activitytracker.ActivityInputBar
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityDescriptor
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualActionsButton
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualComposerSurface
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction
@Composable
@Suppress("LongParameterList")
internal fun TodayBottomPanelContent(
    currentTab: DayManagementTab,
    inputValue: TextFieldValue,
    allTags: List<String>,
    contextMarkerNames: List<String>,
    activityInputText: String,
    activityTagSuggestions: List<String>,
    activityEntityLinks: List<ActivityEntityLink>,
    activityEntityOptions: List<ActivityEntityDescriptor>,
    isActivityOngoing: Boolean,
    journalHoldMenuController: HoldMenu2Controller,
    additionalMoreActions: List<MoreSheetAction>,
    runtimeUiState: DayManagementRuntimeUiState,
    onInputValueChange: (TextFieldValue) -> Unit,
    onActivityTextChange: (String) -> Unit,
    onActivityTagSuggestionClick: (String) -> Unit,
    onActivityEntityLinksChanged: (List<ActivityEntityLink>) -> Unit,
    onToggleActivityStartStop: () -> Unit,
    onTimelessRecordClick: () -> Unit,
    onQuickDoneClick: (String) -> Unit,
    onBackdatedClick: (String) -> Unit,
    onDaySummaryClick: (String) -> Unit,
    onSubmitInput: () -> Unit,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
    themeBudgetPercent: Int,
    onAddDayTheme: () -> Unit,
) {
    val localActions = todayContextualActions(
        currentTab, additionalMoreActions, runtimeUiState, onStartFinalization, onSleep,
    )
    ContextualComposerSurface {
        TodayAutocompleteHost(
            visible = currentTab != DayManagementTab.JOURNAL &&
                currentTab != DayManagementTab.DAY_FOCUSES && currentTab != DayManagementTab.DAY_THEMES,
            inputValue = inputValue, allTags = allTags, contextMarkerNames = contextMarkerNames,
            onInputValueChange = onInputValueChange, modifier = Modifier.fillMaxWidth(),
        )
        if (currentTab == DayManagementTab.JOURNAL) {
            ActivityInputBar(
                text = activityInputText, tagSuggestions = activityTagSuggestions,
                selectedEntityLinks = activityEntityLinks, entityOptions = activityEntityOptions,
                isActivityOngoing = isActivityOngoing, onTextChange = onActivityTextChange,
                onTagSuggestionClick = onActivityTagSuggestionClick,
                onEntityLinksChanged = onActivityEntityLinksChanged,
                onToggleStartStop = onToggleActivityStartStop, onTimelessClick = onTimelessRecordClick,
                onQuickDoneClick = onQuickDoneClick, onBackdatedClick = onBackdatedClick,
                onDaySummaryClick = onDaySummaryClick, holdMenuController = journalHoldMenuController,
                trailingContent = {
                    key(currentTab) {
                        ContextualActionsButton(
                            "Сьогодні · ${currentTab.title}", localActions,
                        )
                    }
                },
            )
        } else if (currentTab != DayManagementTab.DAY_THEMES) {
            BottomPanelComposer(
                inputValue = inputValue, onValueChange = onInputValueChange, onSubmit = onSubmitInput,
                placeholderText = if (currentTab == DayManagementTab.DAY_FOCUSES) "Новий фокус дня..." else "Нове завдання...",
                sendContentDescription = if (currentTab == DayManagementTab.DAY_FOCUSES) "Створити фокус" else "Створити задачу",
                trailingContent = {
                    key(currentTab) {
                        ContextualActionsButton(
                            "Сьогодні · ${currentTab.title}", localActions,
                        )
                    }
                },
                compact = true,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Розподілено $themeBudgetPercent зі 100%",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (themeBudgetPercent > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
                IconButton(onClick = onAddDayTheme, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Додати тему",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                key(currentTab) {
                    ContextualActionsButton(
                        "Сьогодні · ${currentTab.title}", localActions,
                    )
                }
            }
        }
    }
}
