package com.romankozak.forwardappmobile.core.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.NavigationEntry
import com.romankozak.forwardappmobile.data.database.models.NavigationType

private const val CURRENT_ENTRY_ALPHA = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationHistoryMenu(
    navManager: EnhancedNavigationManager,
    onDismiss: () -> Unit,
) {
    val history by navManager.history.collectAsState()
    val currentEntry by navManager.currentEntry.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
        ) {
            Text(
                text = "Історія навігації",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            LazyColumn {
                itemsIndexed(history.reversed()) { reverseIndex, entry ->
                    val actualIndex = history.size - 1 - reverseIndex
                    NavigationHistoryItem(
                        entry = entry,
                        isCurrentEntry = entry == currentEntry,
                        onClick = { navManager.navigateToHistoryEntry(actualIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationHistoryItem(
    entry: NavigationEntry,
    isCurrentEntry: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(entry.title) },
        supportingContent = { Text(entry.type.toDisplayName()) },
        leadingContent = {
            Icon(
                imageVector = entry.type.toIcon(),
                contentDescription = null,
            )
        },
        trailingContent = {
            if (isCurrentEntry) {
                Icon(
                    imageVector = Icons.Outlined.RadioButtonChecked,
                    contentDescription = "Поточна сторінка",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier =
            Modifier
                .clickable(enabled = !isCurrentEntry, onClick = onClick)
                .alpha(if (isCurrentEntry) CURRENT_ENTRY_ALPHA else 1f),
    )
}

private fun NavigationType.toDisplayName(): String =
    when (this) {
        NavigationType.PROJECT_HIERARCHY_SCREEN -> "Головний екран"
        NavigationType.PROJECT_SCREEN -> "Проект"
        NavigationType.GLOBAL_SEARCH -> "Пошук"
        else -> name
    }

private fun NavigationType.toIcon() =
    when (this) {
        NavigationType.PROJECT_HIERARCHY_SCREEN -> Icons.Outlined.Home
        NavigationType.PROJECT_SCREEN -> Icons.Outlined.Folder
        NavigationType.GLOBAL_SEARCH -> Icons.Outlined.Search
        else -> Icons.Outlined.Info
    }
