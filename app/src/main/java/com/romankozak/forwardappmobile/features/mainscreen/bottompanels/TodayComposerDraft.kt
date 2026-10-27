package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab

/** Each Today mode keeps its own draft, including cursor/selection across restoration. */
@Composable
internal fun rememberTodayComposerDraft(currentTab: DayManagementTab): MutableState<TextFieldValue> =
    DayManagementTab.todaySubTabs().associateWith { tab ->
        key(tab.name) { rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) } }
    }.getValue(currentTab)
