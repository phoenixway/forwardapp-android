package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.goaldetail.InputMode

@Composable
fun GoalInputBar(
    inputValue: TextFieldValue,
    inputMode: InputMode,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onModeChangeRequest: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(inputMode) {
        if (inputMode != InputMode.AddGoal) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onModeChangeRequest) {
                Icon(
                    imageVector = when (inputMode) {
                        InputMode.AddGoal -> Icons.Default.Add
                        InputMode.SearchInList, InputMode.SearchGlobal -> Icons.Default.Search
                    },
                    contentDescription = "Change Input Mode"
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                BasicTextField(
                    value = inputValue,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputValue.text.isNotBlank()) {
                            onSubmit()
                        }
                    }),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
                if (inputValue.text.isEmpty()) {
                    Text(
                        text = when (inputMode) {
                            InputMode.AddGoal -> "Додати нову ціль..."
                            InputMode.SearchInList -> "Пошук в цьому списку..."
                            InputMode.SearchGlobal -> "Глобальний пошук..."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedVisibility(
                visible = inputValue.text.isNotBlank(),
                enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
                exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 })
            ) {
                FilledTonalIconButton(onClick = onSubmit) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Submit")
                }
            }
        }
    }
}