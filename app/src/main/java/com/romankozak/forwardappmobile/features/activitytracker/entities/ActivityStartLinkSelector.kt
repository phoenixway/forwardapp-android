package com.romankozak.forwardappmobile.features.activitytracker.entities

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink

@Composable
fun ActivityStartLinkSelector(
    selectedLinks: List<ActivityEntityLink>,
    options: List<ActivityEntityDescriptor>,
    onLinksChanged: (List<ActivityEntityLink>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerVisible by remember { mutableStateOf(false) }

    FilledTonalIconButton(
        onClick = { pickerVisible = true },
        modifier = modifier,
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor =
                    if (selectedLinks.isEmpty()) {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                    },
                contentColor =
                    if (selectedLinks.isEmpty()) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
            ),
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            modifier = Modifier.size(20.dp),
            contentDescription =
                if (selectedLinks.isEmpty()) {
                    "Додати пов’язані сутності"
                } else {
                    "Пов’язаних сутностей: ${selectedLinks.size}"
                },
        )
    }

    if (pickerVisible) {
        ActivityEntityLinkPickerDialog(
            selectedLinks = selectedLinks,
            options = options,
            onLinksChanged = onLinksChanged,
            onDismiss = { pickerVisible = false },
        )
    }
}
