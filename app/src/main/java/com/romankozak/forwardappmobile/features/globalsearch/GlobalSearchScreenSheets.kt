@file:Suppress("WildcardImport")

package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun TypeSelectionList(
    options: List<GlobalSearchType>,
    draft: Set<GlobalSearchType>,
    onSelectAll: () -> Unit,
    onToggleType: (GlobalSearchType) -> Unit,
) {
    ListItem(
        headlineContent = { Text("Усі типи") },
        leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingContent = {
            if (draft.size == options.size) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelectAll),
    )
    options.forEach { type ->
        ListItem(
            headlineContent = { Text(type.label) },
            leadingContent = { Icon(type.icon, contentDescription = null) },
            trailingContent = {
                if (type in draft) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable { onToggleType(type) },
        )
    }
}

@Composable
internal fun TypeBottomSheetActions(
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Скасувати")
        }
        Button(onClick = onApply, modifier = Modifier.weight(1f)) {
            Text("Застосувати")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchPreferencesBottomSheet(
    currentMode: OmniboxMode,
    prefs: OmniboxModeDisplayPrefsState,
    typeOptions: List<GlobalSearchType>,
    selectedTypes: Set<GlobalSearchType>,
    onToggleType: (GlobalSearchType) -> Unit,
    onSelectAllTypes: () -> Unit,
    selectedSort: GlobalSearchSort,
    onSortSelected: (GlobalSearchSort) -> Unit,
    onPreviewChanged: (OmniboxMode, Boolean) -> Unit,
    onRecentsChanged: (OmniboxMode, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Налаштування пошуку",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = {
                    Text("Preview і recents для кожного режиму omnibox.")
                },
                leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
            )
            HorizontalDivider()
            if (currentMode == OmniboxMode.DataSearch) {
                DataSearchSettingsSection(
                    typeOptions = typeOptions,
                    selectedTypes = selectedTypes,
                    onToggleType = onToggleType,
                    onSelectAllTypes = onSelectAllTypes,
                    selectedSort = selectedSort,
                    onSortSelected = onSortSelected,
                )
                HorizontalDivider()
            }
            OmniboxMode.entries.forEach { mode ->
                val modePrefs = prefs[mode]
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = searchSettingsModeTitle(mode),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        headlineContent = { Text("Preview") },
                        supportingContent = { Text("Показувати стартовий/пояснювальний блок режиму") },
                        trailingContent = {
                            Switch(
                                checked = modePrefs.showPreview,
                                onCheckedChange = { onPreviewChanged(mode, it) },
                            )
                        },
                    )
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        headlineContent = { Text("Recents") },
                        supportingContent = { Text("Показувати історію або недавні записи цього режиму") },
                        trailingContent = {
                            Switch(
                                checked = modePrefs.showRecents,
                                onCheckedChange = { onRecentsChanged(mode, it) },
                            )
                        },
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DataSearchSettingsSection(
    typeOptions: List<GlobalSearchType>,
    selectedTypes: Set<GlobalSearchType>,
    onToggleType: (GlobalSearchType) -> Unit,
    onSelectAllTypes: () -> Unit,
    selectedSort: GlobalSearchSort,
    onSortSelected: (GlobalSearchSort) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Фільтри результатів",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = "Все, що забирало місце над списком результатів, тепер тут.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Типи",
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val allSelected = selectedTypes.size == typeOptions.size
            FilterChip(
                selected = allSelected,
                onClick = onSelectAllTypes,
                label = { Text("Усі") },
                leadingIcon = if (allSelected) ({
                    Icon(Icons.Default.Check, contentDescription = null)
                }) else null,
            )
            typeOptions.forEach { type ->
                FilterChip(
                    selected = type in selectedTypes,
                    onClick = { onToggleType(type) },
                    label = { Text(type.label) },
                    leadingIcon = { Icon(type.icon, contentDescription = null) },
                )
            }
        }
        Text(
            text = "Сортування",
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlobalSearchSort.entries.forEach { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { onSortSelected(sort) },
                    label = { Text(sort.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

private fun searchSettingsModeTitle(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Пошук даних"
        OmniboxMode.Command -> "Команди"
        OmniboxMode.QuickCatchInbox -> "Quick catch"
        OmniboxMode.StartActivity -> "Старт активності"
        OmniboxMode.AddActivityEvent -> "Подія активності"
    }
