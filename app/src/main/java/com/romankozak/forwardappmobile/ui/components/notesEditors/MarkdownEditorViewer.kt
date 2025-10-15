package com.romankozak.forwardappmobile.ui.components.notesEditors

import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.romankozak.forwardappmobile.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MarkdownEditorViewer(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    AnimatedContent(
        modifier = modifier,
        targetState = isEditMode,
        label = "MarkdownEditorViewerMode",
        transitionSpec = { fadeIn() with fadeOut() },
    ) { isEditing ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (isEditing) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle =
                        TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (value.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.description_placeholder_with_markdown_hint),
                                style =
                                    TextStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                    ),
                            )
                        }
                        innerTextField()
                    },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                )
            } else {
                val isDark = isSystemInDarkTheme()
                AndroidView(
                    factory = { ctx ->
                        WebViewMarkdownViewer(ctx).apply {
                            layoutParams =
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                        }
                    },
                    update = { viewer ->
                        viewer.renderMarkdown(value.text, isDark)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
