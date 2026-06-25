package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.InputSuggestionActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.AutocompleteSuggestions

@Composable
internal fun TodayAutocompleteHost(
    visible: Boolean,
    inputValue: TextFieldValue,
    allTags: List<String>,
    contextMarkerNames: List<String>,
    onInputValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val inputSuggestionActions = remember { InputSuggestionActions() }
    val autocompleteSuggestions =
        remember(inputValue, allTags, contextMarkerNames) {
            inputSuggestionActions.buildSuggestions(
                currentText = inputValue.text,
                cursorPosition = inputValue.selection.start.coerceAtLeast(0),
                contextMarkerNames = contextMarkerNames,
                tags = allTags,
            )
        }

    AutocompleteSuggestions(
        suggestions = autocompleteSuggestions,
        onSuggestionClick = { suggestion ->
            inputSuggestionActions
                .applySuggestion(
                    currentText = inputValue.text,
                    cursorPosition = inputValue.selection.start.coerceAtLeast(0),
                    suggestion = suggestion,
                )?.let { result ->
                    onInputValueChange(
                        TextFieldValue(
                            text = result.text,
                            selection = TextRange(result.cursorPosition),
                        ),
                    )
                }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
