package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer

@Composable
fun TodayBottomPanelComposer(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    placeholderText: String = "Нове завдання...",
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BottomPanelComposer(
        inputValue = inputValue,
        onValueChange = onValueChange,
        onSubmit = onSubmit,
        placeholderText = placeholderText,
        sendContentDescription = "Створити задачу",
        trailingContent = trailingContent,
        modifier = modifier,
    )
}
