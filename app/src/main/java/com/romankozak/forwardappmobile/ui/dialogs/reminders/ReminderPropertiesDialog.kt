package com.romankozak.forwardappmobile.ui.dialogs.reminders

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.Reminder
import java.util.Calendar

data class ReminderPreset(
    val id: String,
    val label: String,
    val time: Int,
    val unit: String,
    val emoji: String
)

 @Composable
fun ReminderPropertiesDialog(
    onDismiss: () -> Unit,
    onSetReminder: (time: Long) -> Unit,
    onRemoveReminder: (() -> Unit)? = null,
    currentReminders: List<Reminder> = emptyList()
) {
    var selectedPresetId by remember { mutableStateOf<String?>(null) }
    var customValue by remember { mutableStateOf("15") }
    var selectedUnit by remember { mutableStateOf("minutes") }
    var showCustomInput by remember { mutableStateOf(false) }
    var isCustomValid by remember { mutableStateOf(true) }

    val presets = listOf(
        ReminderPreset("soon", "Скоро", 5, "minutes", "⚡"),
        ReminderPreset("30min", "30 хвилин", 30, "minutes", "🕐"),
        ReminderPreset("1hour", "1 година", 1, "hours", "⏰"),
        ReminderPreset("3hours", "3 години", 3, "hours", "📍"),
        ReminderPreset("tomorrow", "Завтра", 1, "days", "📅"),
        ReminderPreset("week", "За тиждень", 7, "days", "📆")
    )

    val unitLabels = mapOf(
        "minutes" to "Хвилини",
        "hours" to "Години",
        "days" to "Дні"
    )

    fun validateCustomValue(value: String): Boolean {
        return value.toIntOrNull()?.let { it in 1..10000 } ?: false
    }

    fun calculateReminderTime(value: Int, unit: String): Long {
        val calendar = Calendar.getInstance()
        when (unit) {
            "minutes" -> calendar.add(Calendar.MINUTE, value)
            "hours" -> calendar.add(Calendar.HOUR_OF_DAY, value)
            "days" -> calendar.add(Calendar.DAY_OF_MONTH, value)
        }
        return calendar.timeInMillis
    }

    fun handleSelectPreset(preset: ReminderPreset) {
        selectedPresetId = preset.id
        showCustomInput = false
    }

    fun handleSetCustom() {
        if (validateCustomValue(customValue)) {
            val reminderTime = calculateReminderTime(customValue.toInt(), selectedUnit)
            onSetReminder(reminderTime)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Встановити нагадування",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    "Виберіть час для нагадування",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Preset Options
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "ПОПУЛЯРНІ ВАРІАНТИ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        presets.forEach { preset ->
                            PresetButton(
                                preset = preset,
                                isSelected = selectedPresetId == preset.id,
                                onClick = { handleSelectPreset(preset) }
                            )
                        }
                    }

                    // Divider with text
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text(
                            "Власний інтервал",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Divider(modifier = Modifier.weight(1f))
                    }

                    // Custom Input Section
                    CustomIntervalSection(
                        expanded = showCustomInput,
                        onExpandedChange = { showCustomInput = it },
                        value = customValue,
                        onValueChange = {
                            customValue = it
                            isCustomValid = validateCustomValue(it)
                        },
                        isValid = isCustomValid,
                        selectedUnit = selectedUnit,
                        onUnitChange = { selectedUnit = it },
                        unitLabels = unitLabels,
                        onSetReminder = { handleSetCustom() }
                    )

                    // Info Box
                    InfoBox()
                }

                // Footer
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Скасувати", fontWeight = FontWeight.SemiBold)
                    }

                    if (selectedPresetId != null && selectedPresetId != "custom") {
                        Button(
                            onClick = {
                                val preset = presets.find { it.id == selectedPresetId }
                                preset?.let {
                                    val reminderTime = calculateReminderTime(it.time, it.unit)
                                    onSetReminder(reminderTime)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Підтвердити", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

 @Composable
private fun PresetButton(
    preset: ReminderPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "preset_bg"
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(enabled = isSelected),
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = preset.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${preset.time} ${when (preset.unit) {
                        "minutes" -> "хв"
                        "hours" -> "год"
                        else -> "днів"
                    }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

 @Composable
private fun CustomIntervalSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    selectedUnit: String,
    onUnitChange: (String) -> Unit,
    unitLabels: Map<String, String>,
    onSetReminder: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!expanded) },
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(enabled = expanded),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (expanded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (expanded) "Введіть вашу тривалість" else "Встановити власний час",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (expanded) "Вкажіть кількість та одиницю" else "Клацніть для розширення",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Divider(modifier = Modifier.fillMaxWidth())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Value Input
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "КІЛЬКІСТЬ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            placeholder = { Text("Введіть число") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = !isValid,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        if (!isValid && value.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Введіть число від 1 до 10000",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Unit Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "ОДИНИЦЯ ЧАСУ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("minutes", "hours", "days").forEach { unit ->
                                Button(
                                    onClick = { onUnitChange(unit) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedUnit == unit)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        unitLabels[unit] ?: unit,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                                        color = if (selectedUnit == unit)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                 }
                            }
                        }
                    }

                    // Preview
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Нагадування через:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "$value ${when (selectedUnit) {
                                    "minutes" -> "хв"
                                    "hours" -> "год"
                                    else -> "днів"
                                }}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Set Button
                    Button(
                        onClick = onSetReminder,
                        enabled = isValid && value.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Встановити нагадування",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = MaterialTheme.typography.labelLarge.fontSize
                        )
                    }
                }
            }
        }
    }
}

 @Composable
private fun InfoBox() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Виберіть готовий варіант або встановіть власний час. Вам буде надіслано сповіщення у вказаний час.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}