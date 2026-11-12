package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactOmnibox(
    modifier: Modifier = Modifier,
    uiState: GlobalSearchUiState,
    currentMode: OmniboxMode,
    modePalette: OmniboxModePalette,
    onShowCreate: () -> Unit,
    onShowRecents: () -> Unit,
    recentsSelected: Boolean,
    onSetMode: (OmniboxMode) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSubmit: () -> Unit,
    onKeyDown: (androidx.compose.ui.input.key.KeyEvent) -> Boolean,
    focusRequester: FocusRequester,
) {
    val isQuickCatchMode = currentMode == OmniboxMode.QuickCatchInbox
    val hasQuery = uiState.query.isNotBlank()

    Surface(
        modifier =
            modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
                    spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f),
                ),
        shape = RoundedCornerShape(22.dp),
        color = modePalette.searchSurface,
        tonalElevation = 5.dp,
        shadowElevation = 5.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = modePalette.panelOutline.copy(alpha = 0.36f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            TextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isQuickCatchMode) Modifier.heightIn(min = 56.dp, max = 144.dp)
                        else Modifier.heightIn(min = 56.dp)
                    )
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { onKeyDown(it) },
                singleLine = !isQuickCatchMode,
                maxLines = if (isQuickCatchMode) 6 else 1,
                placeholder = {
                    Text(
                        placeholderForMode(currentMode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = if (isQuickCatchMode) ImeAction.Default else ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = modePalette.inputFocusedContainer.copy(alpha = 1f),
                    unfocusedContainerColor = modePalette.inputContainer.copy(alpha = 0.98f),
                    disabledContainerColor = modePalette.inputContainer.copy(alpha = 0.98f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = modePalette.iconTint,
                    unfocusedLeadingIconColor = modePalette.iconTint,
                    focusedTrailingIconColor = modePalette.iconTint,
                    unfocusedTrailingIconColor = modePalette.iconTint,
                ),
                trailingIcon = if (hasQuery) {
                    {
                        Row(
                            modifier = Modifier.padding(end = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = onClearQuery,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "очистити",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                onClick = onSubmit,
                                shape = RoundedCornerShape(12.dp),
                                color = modePalette.primaryActionContainer,
                                tonalElevation = 0.dp,
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = submitDescriptionForMode(currentMode),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    null
                },
            )

            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeIconDock(
                    currentMode = currentMode,
                    palette = modePalette,
                    onShowCreate = onShowCreate,
                    onShowRecents = onShowRecents,
                    recentsSelected = recentsSelected,
                    onSetMode = onSetMode,
                )
            }
        }
    }
}

private fun placeholderForMode(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Шукай будь-що..."
        OmniboxMode.Command -> "Команда..."
        OmniboxMode.QuickCatchInbox -> "В Inbox..."
        OmniboxMode.StartActivity -> "Почати активність..."
        OmniboxMode.AddActivityEvent -> "Додати подію..."
    }

private fun submitDescriptionForMode(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Виконати пошук"
        OmniboxMode.Command -> "Виконати команду"
        OmniboxMode.QuickCatchInbox -> "Додати в inbox"
        OmniboxMode.StartActivity -> "Почати активність"
        OmniboxMode.AddActivityEvent -> "Додати подію"
    }
