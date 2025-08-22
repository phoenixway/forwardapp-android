// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/MarkdownEditorViewer.kt
package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MarkdownEditorViewer(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Перемикач режимів: Редактор / Перегляд
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            SegmentedButton(
                selected = isEditMode,
                onClick = { isEditMode = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("Редактор")
            }

            SegmentedButton(
                selected = !isEditMode,
                onClick = { isEditMode = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("Перегляд")
            }
        }

        // Анімований контент: редактор або перегляд
        AnimatedContent(
            targetState = isEditMode,
            label = "MarkdownEditorViewerMode",
            transitionSpec = { fadeIn() with fadeOut() }
        ) { isEditing ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp)
            ) {
                if (isEditing) {
                    // Режим редагування
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace, // Markdown — зручніше писати моноширинним шрифтом
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Placeholder
                                if (value.text.isEmpty() && placeholder != null) {
                                    placeholder()
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp)
                    )

                    // Підказка про Markdown (маленький текст під полем)
                    Text(
                        text = "**жирний**, *курсив*, - список, # заголовок",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                } else {
                    // Режим перегляду
                    AndroidView(
                        factory = { ctx ->
                            WebViewMarkdownViewer(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { viewer ->
                            viewer.renderMarkdown(value.text)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
