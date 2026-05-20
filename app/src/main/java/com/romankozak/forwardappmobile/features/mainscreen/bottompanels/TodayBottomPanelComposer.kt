package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.theme.InputModeColors

@Composable
fun TodayBottomPanelComposer(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    panelStyle: InputModeColors,
    placeholderText: String = "Нове завдання...",
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 3)
                    .defaultMinSize(minHeight = 44.dp),
            shape = RoundedCornerShape(20.dp),
            color = panelStyle.inputFieldColor,
            border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.3f)),
        ) {
            BasicTextField(
                value = inputValue,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = panelStyle.textColor),
                singleLine = false,
                maxLines = 6,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                cursorBrush = SolidColor(panelStyle.textColor),
                decorationBox = { innerTextField ->
                    Box {
                        if (inputValue.text.isBlank()) {
                            Text(
                                text = placeholderText,
                                color = panelStyle.textColor.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedVisibility(
            visible = inputValue.text.isNotBlank(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            IconButton(
                onClick = onSubmit,
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(
                            color = panelStyle.textColor,
                            shape = RoundedCornerShape(22.dp),
                        ),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor =
                            if (panelStyle.textColor.luminance() > 0.5f) {
                                Color.Black
                            } else {
                                Color.White
                            },
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Створити задачу",
                )
            }
        }

        trailingContent?.let { content ->
            Spacer(modifier = Modifier.width(8.dp))
            content()
        }
    }
}
