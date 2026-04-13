@file:Suppress("MagicNumber")

package com.romankozak.forwardappmobile.features.mainscreen.lifemanagement

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.FreshnessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.GeneralStatus
import com.romankozak.forwardappmobile.core.data.models.entities.LifeManagementLevelId
import com.romankozak.forwardappmobile.core.data.models.entities.TransferStatus
import com.romankozak.forwardappmobile.core.theme.ForwardAppMobileTheme

private const val LifeManagementHeaderAlpha = 0.08f
private const val LifeManagementMutedBadgeAlpha = 0.86f
private const val ReadyDarkContainerAlpha = 0.24f
private const val ReadyDarkBorderAlpha = 0.20f
private const val ReadyLightContainerAlpha = 0.42f
private const val SharedBorderAlpha = 0.14f
private const val ConditionalDarkContainerAlpha = 0.24f
private const val ConditionalDarkBorderAlpha = 0.18f
private const val ConditionalLightContainerAlpha = 0.40f
private const val BlockedContainerAlpha = 0.34f
private const val BlockedBorderAlpha = 0.18f
private const val FallbackDarkContainerAlpha = 0.82f
private const val FallbackDarkBorderAlpha = 0.26f
private const val FallbackLightContainerAlpha = 0.05f
private const val FallbackLightBorderAlpha = 0.12f

private val ReadyDarkContainerColor = Color(0xFF23462D)
private val ReadyDarkContentColor = Color(0xFFB8E6C4)
private val ReadyLightContainerColor = Color(0xFFDDF1E3)
private val ReadyLightContentColor = Color(0xFF215A31)
private val ConditionalDarkContainerColor = Color(0xFF4B3B12)
private val ConditionalDarkContentColor = Color(0xFFFFDF8A)
private val ConditionalLightContainerColor = Color(0xFFFFEDBF)
private val ConditionalLightContentColor = Color(0xFF8A5B00)

@Composable
fun LifeManagementStatusPanelSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LifeManagementStatusPanelViewModel = hiltViewModel(),
) {
    val statuses by viewModel.statuses.collectAsStateWithLifecycle()
    LifeManagementStatusPanelSection(
        statuses = statuses,
        isExpanded = isExpanded,
        onToggleExpanded = onToggleExpanded,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
fun LifeManagementStatusPanelSection(
    statuses: List<LifeManagementLevelStatus>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSave: (LifeManagementLevelStatusUpdate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingLevel by remember { mutableStateOf<LifeManagementLevelStatus?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LifeManagementSectionHeader(
            title = "Level Status",
            subtitle =
                if (isExpanded) {
                    if (statuses.isEmpty()) "Initializing 8 levels" else "${statuses.size} levels"
                } else {
                    statuses.collapsedSubtitle()
                },
            isExpanded = isExpanded,
            onClick = onToggleExpanded,
        )

        if (isExpanded) {
            if (statuses.isEmpty()) {
                LifeManagementEmptyCard()
            } else {
                statuses.forEach { status ->
                    LifeManagementStatusCard(
                        status = status,
                        onClick = { editingLevel = status },
                    )
                }
            }
        }
    }

    editingLevel?.let { status ->
        LifeManagementStatusEditDialog(
            initialStatus = status,
            onDismiss = { editingLevel = null },
            onSave = { update ->
                onSave(update)
                editingLevel = null
            },
        )
    }
}

@Composable
private fun LifeManagementSectionHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = LifeManagementHeaderAlpha),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Icon(
                imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LifeManagementStatusCard(
    status: LifeManagementLevelStatus,
    onClick: () -> Unit,
) {
    val cardColors = rememberStatusCardColors(status.generalStatus)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColors.containerColor),
        border = BorderStroke(1.dp, cardColors.borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = status.levelId.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                StatusBadge(status.generalStatus)
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                StatusLine(prefix = "↓ transfer", value = status.transferStatus.displayName())
                StatusLine(prefix = "⟳ freshness", value = status.freshnessStatus.displayName())
                if (status.blockerText.isNotBlank()) {
                    StatusLine(prefix = "! blocker", value = status.blockerText)
                }
                if (status.nextActionText.isNotBlank()) {
                    StatusLine(prefix = "→ next", value = status.nextActionText)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(
    prefix: String,
    value: String,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "$prefix:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StatusBadge(status: GeneralStatus) {
    val colors = rememberStatusBadgeColors(status)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = colors.containerColor,
        contentColor = colors.contentColor,
    ) {
        Text(
            text = status.displayName(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun rememberStatusBadgeColors(status: GeneralStatus): StatusBadgeColors =
    when {
        status == GeneralStatus.READY && isSystemInDarkTheme() ->
            StatusBadgeColors(
                containerColor = ReadyDarkContainerColor,
                contentColor = ReadyDarkContentColor,
            )
        status == GeneralStatus.READY ->
            StatusBadgeColors(
                containerColor = ReadyLightContainerColor,
                contentColor = ReadyLightContentColor,
            )
        status == GeneralStatus.CONDITIONAL && isSystemInDarkTheme() ->
            StatusBadgeColors(
                containerColor = ConditionalDarkContainerColor,
                contentColor = ConditionalDarkContentColor,
            )
        status == GeneralStatus.CONDITIONAL ->
            StatusBadgeColors(
                containerColor = ConditionalLightContainerColor,
                contentColor = ConditionalLightContentColor,
            )
        status == GeneralStatus.BLOCKED ->
            StatusBadgeColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        isSystemInDarkTheme() ->
            StatusBadgeColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        else ->
            StatusBadgeColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = LifeManagementMutedBadgeAlpha),
                contentColor = MaterialTheme.colorScheme.surface,
            )
    }

@Composable
private fun rememberStatusCardColors(status: GeneralStatus): StatusCardColors {
    val baseContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    return when {
        status == GeneralStatus.READY && isSystemInDarkTheme() ->
            StatusCardColors(
                containerColor =
                    ReadyDarkContainerColor.copy(alpha = ReadyDarkContainerAlpha)
                        .compositeOver(baseContainer),
                borderColor = ReadyDarkContentColor.copy(alpha = ReadyDarkBorderAlpha),
            )
        status == GeneralStatus.READY ->
            StatusCardColors(
                containerColor =
                    ReadyLightContainerColor.copy(alpha = ReadyLightContainerAlpha)
                        .compositeOver(baseContainer),
                borderColor = ReadyLightContentColor.copy(alpha = SharedBorderAlpha),
            )
        status == GeneralStatus.CONDITIONAL && isSystemInDarkTheme() ->
            StatusCardColors(
                containerColor =
                    ConditionalDarkContainerColor.copy(alpha = ConditionalDarkContainerAlpha).compositeOver(baseContainer),
                borderColor = ConditionalDarkContentColor.copy(alpha = ConditionalDarkBorderAlpha),
            )
        status == GeneralStatus.CONDITIONAL ->
            StatusCardColors(
                containerColor =
                    ConditionalLightContainerColor.copy(alpha = ConditionalLightContainerAlpha).compositeOver(baseContainer),
                borderColor = ConditionalLightContentColor.copy(alpha = SharedBorderAlpha),
            )
        status == GeneralStatus.BLOCKED ->
            StatusCardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = BlockedContainerAlpha)
                        .compositeOver(baseContainer),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = BlockedBorderAlpha),
            )
        isSystemInDarkTheme() ->
            StatusCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = FallbackDarkContainerAlpha),
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = FallbackDarkBorderAlpha),
            )
        else ->
            StatusCardColors(
                containerColor =
                    MaterialTheme.colorScheme.onSurface.copy(alpha = FallbackLightContainerAlpha)
                        .compositeOver(baseContainer),
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = FallbackLightBorderAlpha),
            )
    }
}

@Composable
private fun LifeManagementEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Text(
            text = "Preparing initial status levels…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LifeManagementStatusEditDialog(
    initialStatus: LifeManagementLevelStatus,
    onDismiss: () -> Unit,
    onSave: (LifeManagementLevelStatusUpdate) -> Unit,
) {
    var generalStatus by remember(initialStatus.levelId) { mutableStateOf(initialStatus.generalStatus) }
    var transferStatus by remember(initialStatus.levelId) { mutableStateOf(initialStatus.transferStatus) }
    var freshnessStatus by remember(initialStatus.levelId) { mutableStateOf(initialStatus.freshnessStatus) }
    var blockerText by remember(initialStatus.levelId) { mutableStateOf(initialStatus.blockerText) }
    var nextActionText by remember(initialStatus.levelId) { mutableStateOf(initialStatus.nextActionText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = initialStatus.levelId.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Edit operational status",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EnumSelector(
                    label = "General status",
                    selected = generalStatus,
                    options = GeneralStatus.entries,
                    optionLabel = { it.displayName() },
                    onSelected = { generalStatus = it },
                )
                EnumSelector(
                    label = "Transfer",
                    selected = transferStatus,
                    options = TransferStatus.entries,
                    optionLabel = { it.displayName() },
                    onSelected = { transferStatus = it },
                )
                EnumSelector(
                    label = "Freshness",
                    selected = freshnessStatus,
                    options = FreshnessStatus.entries,
                    optionLabel = { it.displayName() },
                    onSelected = { freshnessStatus = it },
                )
                OutlinedTextField(
                    value = blockerText,
                    onValueChange = { blockerText = it },
                    label = { Text("Blocker") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                OutlinedTextField(
                    value = nextActionText,
                    onValueChange = { nextActionText = it },
                    label = { Text("Next action") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        LifeManagementLevelStatusUpdate(
                            levelId = initialStatus.levelId,
                            generalStatus = generalStatus,
                            transferStatus = transferStatus,
                            freshnessStatus = freshnessStatus,
                            blockerText = blockerText,
                            nextActionText = nextActionText,
                        ),
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumSelector(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun GeneralStatus.displayName(): String =
    when (this) {
        GeneralStatus.READY -> "ready"
        GeneralStatus.CONDITIONAL -> "conditional"
        GeneralStatus.BLOCKED -> "blocked"
        GeneralStatus.DEFECTED -> "defected"
    }

private fun TransferStatus.displayName(): String =
    when (this) {
        TransferStatus.NONE -> "none"
        TransferStatus.PARTIAL -> "partial"
        TransferStatus.COMPLETE -> "complete"
    }

private fun FreshnessStatus.displayName(): String =
    when (this) {
        FreshnessStatus.OUTDATED -> "outdated"
        FreshnessStatus.NEEDS_REVIEW -> "needs review"
        FreshnessStatus.FRESH -> "fresh"
        FreshnessStatus.OUTDATED_BY_PARENT -> "outdated by parent"
    }

private fun List<LifeManagementLevelStatus>.collapsedSubtitle(): String {
    if (isEmpty()) return "Initializing 8 levels"

    val highestProblemLevel =
        asSequence()
            .sortedBy { it.levelId.order }
            .firstOrNull { it.generalStatus == GeneralStatus.DEFECTED || it.generalStatus == GeneralStatus.BLOCKED }

    return when (highestProblemLevel?.generalStatus) {
        GeneralStatus.DEFECTED -> "Defected: ${highestProblemLevel.levelId.label}"
        GeneralStatus.BLOCKED -> "Blocked: ${highestProblemLevel.levelId.label}"
        else -> "No blocked or defected levels"
    }
}

private data class StatusBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
)

private data class StatusCardColors(
    val containerColor: Color,
    val borderColor: Color,
)

private val previewStatuses =
    listOf(
        LifeManagementLevelStatus(
            levelId = LifeManagementLevelId.MAIN_BEACONS,
            generalStatus = GeneralStatus.READY,
            transferStatus = TransferStatus.COMPLETE,
            freshnessStatus = FreshnessStatus.FRESH,
            blockerText = "",
            nextActionText = "maintain cadence",
            updatedAt = 0L,
        ),
        LifeManagementLevelStatus(
            levelId = LifeManagementLevelId.STRATEGIC_PROJECTING_OF_MAIN_BEACONS,
            generalStatus = GeneralStatus.CONDITIONAL,
            transferStatus = TransferStatus.PARTIAL,
            freshnessStatus = FreshnessStatus.OUTDATED_BY_PARENT,
            blockerText = "mandatory core changed",
            nextActionText = "resync strategic projecting",
            updatedAt = 0L,
        ),
        LifeManagementLevelStatus(
            levelId = LifeManagementLevelId.DAY,
            generalStatus = GeneralStatus.BLOCKED,
            transferStatus = TransferStatus.NONE,
            freshnessStatus = FreshnessStatus.OUTDATED,
            blockerText = "week plan not reconciled",
            nextActionText = "rebuild today from current week",
            updatedAt = 0L,
        ),
    )

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun LifeManagementStatusPanelPreview() {
    ForwardAppMobileTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LifeManagementStatusPanelSection(
                statuses = previewStatuses,
                isExpanded = true,
                onToggleExpanded = {},
                onSave = {},
            )
        }
    }
}
