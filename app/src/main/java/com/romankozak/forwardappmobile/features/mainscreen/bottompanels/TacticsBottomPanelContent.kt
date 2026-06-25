package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Link
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.AutocompleteSuggestions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelActionRow
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalRail
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelIconButton
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelSurface

@Composable
internal fun TacticsBottomPanelContent(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    autocompleteSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onAddMissionFromContext: () -> Unit,
    onToggleScopeLinksSheet: () -> Unit,
    globalActions: BottomPanelGlobalActions,
) {
    BottomPanelSurface {
        BottomPanelActionRow(
            leadingContent = {
                BottomPanelIconButton(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Додати місію",
                    onClick = onAddMissionFromContext,
                )
                BottomPanelIconButton(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = "Показати зв'язки",
                    onClick = onToggleScopeLinksSheet,
                )
            },
            trailingContent = {
                BottomPanelGlobalRail(
                    actions = globalActions,
                )
            },
        )

        AutocompleteSuggestions(
            suggestions = autocompleteSuggestions,
            onSuggestionClick = onSuggestionClick,
            modifier = Modifier.fillMaxWidth(),
        )

        BottomPanelComposer(
            inputValue = inputValue,
            onValueChange = onValueChange,
            onSubmit = onSubmit,
            placeholderText = placeholder,
            sendContentDescription = "Створити місію",
        )
    }
}
