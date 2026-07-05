package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.goalproperties.LinksTabContent
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import kotlin.math.roundToInt

private enum class DayFocusEditorTab(
    val title: String,
) {
    GENERAL("General"),
    LINKS("Links"),
}

private enum class DayFocusBudgetInputMode {
    PERCENT,
    HOURS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayFocusItemEditorSheet(
    initialType: DayFocusType,
    availableContexts: List<ProjectOption>,
    availableAttachments: List<AttachmentOption>,
    existingItem: DayFocusItem? = null,
    otherBudgetPercent: Int = 0,
    predictedDayDurationMinutes: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, List<RelatedLink>, DayFocusType, Boolean, Int?) -> Unit,
    onCreateDocumentForPicker: suspend (NewDocumentDraft) -> String?,
) {
    var title by remember(existingItem?.id) { mutableStateOf(existingItem?.title.orEmpty()) }
    var notes by remember(existingItem?.id) { mutableStateOf(existingItem?.notes.orEmpty()) }
    var selectedType by remember(existingItem?.id, initialType) { mutableStateOf(existingItem?.type ?: initialType) }
    var isEveryday by remember(existingItem?.id) { mutableStateOf(existingItem?.isEveryday == true) }
    var budgetPercentText by remember(existingItem?.id) {
        mutableStateOf(existingItem?.budgetPercent?.toString().orEmpty())
    }
    var budgetInputMode by remember(existingItem?.id) { mutableStateOf(DayFocusBudgetInputMode.PERCENT) }
    val normalizedDayDurationMinutes = predictedDayDurationMinutes?.takeIf { it > 0L }
    val currentBudgetPercent =
        when (budgetInputMode) {
            DayFocusBudgetInputMode.PERCENT -> budgetPercentText.toBudgetPercentOrNull()
            DayFocusBudgetInputMode.HOURS -> budgetPercentText.toBudgetPercentFromHours(normalizedDayDurationMinutes)
        }
    val isBudgetValid = budgetPercentText.isBlank() || currentBudgetPercent != null
    val projectedBudgetPercent = otherBudgetPercent + (currentBudgetPercent ?: 0)
    val isProjectedBudgetOverLimit = projectedBudgetPercent > 100
    var selectedTab by remember(existingItem?.id) { mutableStateOf(DayFocusEditorTab.GENERAL) }
    var links by remember(existingItem?.id) { mutableStateOf(existingItem?.relatedLinks.orEmpty()) }
    var activePickerTab by remember(existingItem?.id) { mutableStateOf<LinkPickerTab?>(null) }
    var pendingCreateAction by remember(existingItem?.id) { mutableStateOf<PickerCreateAction?>(null) }
    var pendingAttachmentId by remember(existingItem?.id) { mutableStateOf<String?>(null) }

    val selectedAttachmentIds =
        remember(links, availableAttachments) {
            val selectedLinkKeys = links.map(::relatedLinkIdentity).toSet()
            availableAttachments
                .filter { option -> option.toRelatedLink()?.let(::relatedLinkIdentity) in selectedLinkKeys }
                .mapTo(mutableSetOf()) { it.id }
        }

    LaunchedEffect(pendingAttachmentId, availableAttachments) {
        val attachmentId = pendingAttachmentId ?: return@LaunchedEffect
        val option = availableAttachments.firstOrNull { it.id == attachmentId } ?: return@LaunchedEffect
        val link = option.toRelatedLink() ?: return@LaunchedEffect
        links = links.appendLink(link)
        pendingAttachmentId = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            DayFocusEditorHeader(
                title = if (existingItem == null) "Новий фокус дня" else "Редагувати фокус дня",
                onDismiss = onDismiss,
            )

            TabRow(selectedTabIndex = selectedTab.ordinal) {
                DayFocusEditorTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                        icon = {
                            Icon(
                                imageVector = if (tab == DayFocusEditorTab.GENERAL) Icons.Default.Settings else Icons.Default.Link,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                DayFocusEditorTab.GENERAL ->
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DayFocusType.entries.forEach { type ->
                                    FilterChip(
                                        selected = selectedType == type,
                                        onClick = { selectedType = type },
                                        label = { Text(type.title()) },
                                    )
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Назва") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Нотатки") },
                                minLines = 4,
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = budgetInputMode == DayFocusBudgetInputMode.PERCENT,
                                        onClick = {
                                            budgetPercentText = currentBudgetPercent?.toString().orEmpty()
                                            budgetInputMode = DayFocusBudgetInputMode.PERCENT
                                        },
                                        label = { Text("%") },
                                    )
                                    FilterChip(
                                        selected = budgetInputMode == DayFocusBudgetInputMode.HOURS,
                                        enabled = normalizedDayDurationMinutes != null,
                                        onClick = {
                                            budgetPercentText =
                                                currentBudgetPercent
                                                    ?.toHoursText(normalizedDayDurationMinutes)
                                                    .orEmpty()
                                            budgetInputMode = DayFocusBudgetInputMode.HOURS
                                        },
                                        label = { Text("год") },
                                    )
                                }
                                OutlinedTextField(
                                    value = budgetPercentText,
                                    onValueChange = { input ->
                                        budgetPercentText =
                                            when (budgetInputMode) {
                                                DayFocusBudgetInputMode.PERCENT -> input.filter(Char::isDigit).take(3)
                                                DayFocusBudgetInputMode.HOURS -> input.toHoursInput()
                                            }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = {
                                        Text(
                                            if (budgetInputMode == DayFocusBudgetInputMode.PERCENT) {
                                                "Ліміт часу на сьогодні, %"
                                            } else {
                                                "Ліміт часу на сьогодні, год"
                                            },
                                        )
                                    },
                                    supportingText = {
                                        Text(
                                            text =
                                                buildBudgetSupportingText(
                                                    projectedBudgetPercent = projectedBudgetPercent,
                                                    currentBudgetPercent = currentBudgetPercent,
                                                    predictedDayDurationMinutes = normalizedDayDurationMinutes,
                                                ),
                                            color =
                                                if (isProjectedBudgetOverLimit) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                        )
                                    },
                                    isError = !isBudgetValid || isProjectedBudgetOverLimit,
                                    singleLine = true,
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType =
                                                if (budgetInputMode == DayFocusBudgetInputMode.PERCENT) {
                                                    KeyboardType.Number
                                                } else {
                                                    KeyboardType.Decimal
                                                },
                                            imeAction = ImeAction.Next,
                                        ),
                                )
                            }
                        }
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .selectable(selected = isEveryday, onClick = { isEveryday = !isEveryday })
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Checkbox(
                                        checked = isEveryday,
                                        onCheckedChange = { isEveryday = it },
                                    )
                                    Column {
                                        Text("Everyday", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "З'являтиметься в наступні дні",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                DayFocusEditorTab.LINKS ->
                    LinksTabContent(
                        links = links,
                        onAddProjectLink = {
                            activePickerTab = LinkPickerTab.CONTEXTS
                            pendingCreateAction = null
                        },
                        onAddDocumentLink = {
                            activePickerTab = LinkPickerTab.ATTACHMENTS
                            pendingCreateAction = null
                        },
                        onAddWebLink = {
                            activePickerTab = LinkPickerTab.ATTACHMENTS
                            pendingCreateAction = PickerCreateAction.WEB_LINK
                        },
                        onAddObsidianLink = {
                            activePickerTab = LinkPickerTab.ATTACHMENTS
                            pendingCreateAction = PickerCreateAction.OBSIDIAN
                        },
                        onRemoveLink = { identity ->
                            links = links.filterNot { link -> relatedLinkIdentity(link) == identity || link.target == identity }
                        },
                    )
            }

            HorizontalDivider()

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Скасувати")
                }
                Button(
                    onClick = {
                        onConfirm(
                            title,
                            notes,
                            links,
                            selectedType,
                            isEveryday,
                            currentBudgetPercent,
                        )
                    },
                    enabled = title.isNotBlank() && isBudgetValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Зберегти")
                }
            }
        }
    }

    activePickerTab?.let { initialTab ->
        LinkedTargetsPickerDialog(
            contextOptions = availableContexts,
            attachmentOptions = availableAttachments,
            preselectedContextIds = links.filter { it.type == LinkType.CONTEXT }.mapTo(mutableSetOf()) { it.target },
            preselectedAttachmentIds = selectedAttachmentIds,
            initialTab = initialTab,
            initialCreateAction = pendingCreateAction,
            onDismiss = {
                activePickerTab = null
                pendingCreateAction = null
            },
            onContextSelected = { id ->
                val contextOption = availableContexts.firstOrNull { it.id == id }
                val link = RelatedLink(type = LinkType.CONTEXT, target = id, displayName = contextOption?.name)
                links = links.appendLink(link)
                activePickerTab = null
                pendingCreateAction = null
            },
            onAttachmentSelected = { id ->
                val option = availableAttachments.firstOrNull { it.id == id }
                val link = option?.toRelatedLink()
                if (link != null) {
                    links = links.appendLink(link)
                } else {
                    pendingAttachmentId = id
                }
                activePickerTab = null
                pendingCreateAction = null
            },
            onCreateRootContext = null,
            onCreateDocument = onCreateDocumentForPicker,
        )
    }
}

@Composable
private fun DayFocusEditorHeader(
    title: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Налаштуй зміст, тип і пов'язані матеріали",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Закрити",
            )
        }
    }
}

private fun List<RelatedLink>.appendLink(link: RelatedLink): List<RelatedLink> =
    if (any { relatedLinkIdentity(it) == relatedLinkIdentity(link) }) this else this + link

private fun AttachmentOption.toRelatedLink(): RelatedLink? =
    when {
        linkType == LinkType.URL && !target.isNullOrBlank() ->
            RelatedLink(type = LinkType.URL, target = target, displayName = name)
        linkType == LinkType.OBSIDIAN && !target.isNullOrBlank() ->
            RelatedLink(type = LinkType.OBSIDIAN, target = target, displayName = name, vault = vault)
        attachmentType == "NOTE_DOCUMENT" && !entityId.isNullOrBlank() ->
            RelatedLink(type = LinkType.NOTE_DOCUMENT, target = entityId, displayName = name)
        attachmentType == "JOURNAL_DOCUMENT" && !entityId.isNullOrBlank() ->
            RelatedLink(type = LinkType.JOURNAL_DOCUMENT, target = entityId, displayName = name)
        attachmentType == "CHECKLIST" && !entityId.isNullOrBlank() ->
            RelatedLink(type = LinkType.CHECKLIST, target = entityId, displayName = name)
        attachmentType == "MUSIC_NOTE" && !entityId.isNullOrBlank() ->
            RelatedLink(type = LinkType.MUSIC_NOTE, target = entityId, displayName = name)
        else -> null
    }

private fun relatedLinkIdentity(link: RelatedLink): String = "${link.type}:${link.target}:${link.vault.orEmpty()}"

private fun DayFocusType.title(): String =
    when (this) {
        DayFocusType.FOCUS -> "Фокус"
        DayFocusType.RESPONSIBILITY -> "Зона відповідальності"
    }

private fun String.toBudgetPercentOrNull(): Int? =
    trim()
        .takeIf { it.isNotEmpty() }
        ?.toIntOrNull()
        ?.takeIf { it in 0..100 }

private fun String.toBudgetPercentFromHours(dayDurationMinutes: Long?): Int? {
    val duration = dayDurationMinutes?.takeIf { it > 0L } ?: return null
    val hours = trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (hours < 0.0) return null
    return ((hours * 60.0 / duration) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
}

private fun Int.toHoursText(dayDurationMinutes: Long?): String {
    val duration = dayDurationMinutes?.takeIf { it > 0L } ?: return ""
    val hours = duration * this / 100.0 / 60.0
    return if (hours % 1.0 == 0.0) {
        hours.toInt().toString()
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f", hours)
    }
}

private fun String.toHoursInput(): String {
    var hasDecimalSeparator = false
    return buildString {
        this@toHoursInput.forEach { char ->
            when {
                char.isDigit() -> append(char)
                (char == '.' || char == ',') && !hasDecimalSeparator -> {
                    append(char)
                    hasDecimalSeparator = true
                }
            }
        }
    }.take(5)
}

private fun buildBudgetSupportingText(
    projectedBudgetPercent: Int,
    currentBudgetPercent: Int?,
    predictedDayDurationMinutes: Long?,
): String {
    val current = currentBudgetPercent ?: 0
    val hoursText =
        current
            .takeIf { predictedDayDurationMinutes != null }
            ?.toHoursText(predictedDayDurationMinutes)
            ?.takeIf { it.isNotBlank() }
            ?.let { " · поточний: $current% / $it год" }
            .orEmpty()
    return "Сума фокусів і зон: $projectedBudgetPercent/100$hoursText"
}
