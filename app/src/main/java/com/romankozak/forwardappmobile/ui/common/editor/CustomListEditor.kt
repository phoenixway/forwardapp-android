package com.romankozak.forwardappmobile.ui.common.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.customlist.UnifiedCustomListUiState
import com.romankozak.forwardappmobile.ui.screens.customlist.UnifiedCustomListViewModel

@Composable
fun CreateEditContent(
  uiState: UnifiedCustomListUiState,
  viewModel: UnifiedCustomListViewModel,
  paddingValues: PaddingValues,
  titleFocusRequester: FocusRequester,
  contentFocusRequester: FocusRequester,
  isCreating: Boolean,
) {
  Column(
    modifier = 
      Modifier.fillMaxSize()
        .padding(start = 16.dp, end = 16.dp, bottom = paddingValues.calculateBottomPadding())
        .verticalScroll(rememberScrollState())
        .imePadding()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    // Title Section (показуємо тільки при створенні або якщо потрібно редагувати назву)
    if (isCreating || !uiState.isExistingList) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = 
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Title,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "List Title",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            modifier = Modifier.fillMaxWidth().focusRequester(titleFocusRequester),
            placeholder = {
              Text("Enter list title...", style = MaterialTheme.typography.bodyLarge)
            },
            singleLine = true,
            isError = uiState.error != null,
            colors = 
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                errorBorderColor = MaterialTheme.colorScheme.error,
              ),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
          )

          AnimatedVisibility(
            visible = uiState.error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
          ) {
            val error = uiState.error
            if (error != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Error,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = error,
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }
        }
      }
    }

    // Content Section
    Card(
      modifier = Modifier.fillMaxWidth().weight(1f),
      colors = 
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = 
              Modifier.size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "✏",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            text = "List Content",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        BasicTextField(
          value = uiState.content,
          onValueChange = { newText ->
            val oldText = uiState.content
            viewModel.onContentChange(newText)

            // Handle Enter key - auto add bullet points
            if (
              newText.text.length > oldText.text.length &&
                oldText.selection.end < newText.text.length &&
                newText.text[oldText.selection.start] == '\n'
            ) {

              val lineStart = 
                newText.text.lastIndexOf('\n', startIndex = oldText.selection.start - 1) + 1
              val previousLine = newText.text.substring(lineStart, oldText.selection.start)
              val leadingWhitespace = previousLine.takeWhile { it.isWhitespace() }

              if (previousLine.trim().startsWith("• ")) {
                val listMarker = "• "
                val newCursorPos = 
                  newText.selection.start + leadingWhitespace.length + listMarker.length
                val finalText = 
                  newText.text.substring(0, newText.selection.start) +
                    leadingWhitespace +
                    listMarker +
                    newText.text.substring(newText.selection.start)
                viewModel.onContentChange(
                  TextFieldValue(finalText, selection = TextRange(newCursorPos))
                )
              }
            }
          },
          modifier = Modifier.fillMaxSize().focusRequester(contentFocusRequester),
          textStyle = 
            TextStyle(
              fontSize = MaterialTheme.typography.bodyLarge.fontSize,
              lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
              color = MaterialTheme.colorScheme.onSurface,
            ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
          decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
              if (uiState.content.text.isEmpty()) {
                Text(
                  text = 
                    "• Start typing your list items\n• Each line can be a new item\n• Use bullet points for better organization",
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
              }
              innerTextField()
            }
          },
        )
      }
    }

    Spacer(modifier = Modifier.height(80.dp))
  }
}
