package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun BottomPanelComposer(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    maxHeightScreenFraction: Int = 3,
    sendContentDescription: String = "Створити",
    secondarySubmitActions: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = bottomPanelColors()
    Row(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = BottomPanelTokens.ComposerMinHeight),
        verticalAlignment = Alignment.Bottom,
    ) {
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / maxHeightScreenFraction)
                    .defaultMinSize(minHeight = BottomPanelTokens.InputMinHeight),
            shape = RoundedCornerShape(BottomPanelTokens.InputCornerRadius),
            color = colors.inputContainer,
            border = BorderStroke(BottomPanelTokens.BorderWidth, colors.inputBorder),
        ) {
            BasicTextField(
                value = inputValue,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = BottomPanelTokens.InputHorizontalPadding,
                            vertical = BottomPanelTokens.InputVerticalPadding,
                        ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.content),
                singleLine = false,
                maxLines = 6,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                cursorBrush = SolidColor(colors.primaryActionContainer),
                decorationBox = { innerTextField ->
                    Box {
                        if (inputValue.text.isBlank()) {
                            Text(
                                text = placeholderText,
                                color = colors.mutedContent.copy(alpha = BottomPanelTokens.PlaceholderAlpha),
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
            Row {
                secondarySubmitActions?.let { content ->
                    content()
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(
                    onClick = onSubmit,
                    modifier =
                        Modifier
                            .size(BottomPanelTokens.PrimaryActionButtonSize)
                            .background(
                                color = colors.primaryActionContainer,
                                shape = RoundedCornerShape(BottomPanelTokens.PrimaryActionButtonSize / 2),
                            ),
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            contentColor = colors.primaryActionContent,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = sendContentDescription,
                        modifier = Modifier.size(BottomPanelTokens.ActionIconSize),
                    )
                }
            }
        }

        trailingContent?.let { content ->
            Spacer(modifier = Modifier.width(8.dp))
            content()
        }
    }
}
