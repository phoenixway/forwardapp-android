package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.activitytracker.ActivityInputBar
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelMoreActionButton
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelSurface
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.MoreSheetAction

@Composable
@Suppress("LongParameterList")
internal fun TodayBottomPanelContent(
    currentTab: DayManagementTab,
    inputValue: TextFieldValue,
    allTags: List<String>,
    contextMarkerNames: List<String>,
    activityInputText: String,
    isActivityOngoing: Boolean,
    journalHoldMenuController: HoldMenu2Controller,
    globalActions: BottomPanelGlobalActions,
    additionalMoreActions: List<MoreSheetAction>,
    runtimeUiState: DayManagementRuntimeUiState,
    onInputValueChange: (TextFieldValue) -> Unit,
    onActivityTextChange: (String) -> Unit,
    onToggleActivityStartStop: () -> Unit,
    onTimelessRecordClick: () -> Unit,
    onQuickDoneClick: (String) -> Unit,
    onDaySummaryClick: (String) -> Unit,
    onSubmitInput: () -> Unit,
    onSelectTodayTab: (DayManagementTab) -> Unit,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
) {
    BottomPanelSurface {
        TodayAutocompleteHost(
            visible = currentTab != DayManagementTab.JOURNAL && currentTab != DayManagementTab.DAY_FOCUSES,
            inputValue = inputValue,
            allTags = allTags,
            contextMarkerNames = contextMarkerNames,
            onInputValueChange = onInputValueChange,
            modifier = Modifier.fillMaxWidth(),
        )
        if (currentTab == DayManagementTab.JOURNAL) {
            ActivityInputBar(
                text = activityInputText,
                isActivityOngoing = isActivityOngoing,
                onTextChange = onActivityTextChange,
                onToggleStartStop = onToggleActivityStartStop,
                onTimelessClick = onTimelessRecordClick,
                onQuickDoneClick = onQuickDoneClick,
                onDaySummaryClick = onDaySummaryClick,
                holdMenuController = journalHoldMenuController,
                showMoreMenu = false,
                trailingContent = {
                    BottomPanelMoreActionButton(
                        actions = globalActions,
                        additionalActions = additionalMoreActions,
                    )
                },
            )
        } else {
            TodayBottomPanelComposer(
                inputValue = inputValue,
                onValueChange = onInputValueChange,
                onSubmit = onSubmitInput,
                placeholderText =
                    if (currentTab == DayManagementTab.DAY_FOCUSES) {
                        "Новий фокус дня..."
                    } else {
                        "Нове завдання..."
                    },
                trailingContent = {
                    BottomPanelMoreActionButton(
                        actions = globalActions,
                        additionalActions = additionalMoreActions,
                    )
                },
            )
        }

        TodaySubTabs(
            selectedTab = currentTab,
            onTabSelected = onSelectTodayTab,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        )
        TodayBottomPanelRuntimeActions(
            currentTab = currentTab,
            runtimeUiState = runtimeUiState,
            onStartFinalization = onStartFinalization,
            onSleep = onSleep,
        )
    }
}
