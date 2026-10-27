package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer

@Composable
fun TodayBottomPanelComposer(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    placeholderText: String = "Нове завдання...",
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actionsAboveInput: Boolean = false,
) {
    if (actionsAboveInput) {
        Column(modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (inputValue.text.isNotBlank()) IconButton(onClick = onSubmit, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Створити задачу", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                trailingContent?.invoke()
            }
            BottomPanelComposer(
                inputValue = inputValue,
                onValueChange = onValueChange,
                onSubmit = onSubmit,
                placeholderText = placeholderText,
                sendContentDescription = "Створити задачу",
                showSubmitAction = false,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        return
    }
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
