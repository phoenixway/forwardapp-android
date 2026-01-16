package com.romankozak.forwardappmobile.features.reminders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.reminders.model.ReminderType

@Composable
fun ReminderTypeSelector(
    selectedType: ReminderType,
    onTypeSelected: (ReminderType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Тип нагадування:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReminderTypeItem(
                title = "Швидкий вибір",
                subtitle = "Типові інтервали часу",
                isSelected = selectedType == ReminderType.QUICK_DURATION,
                onClick = { onTypeSelected(ReminderType.QUICK_DURATION) },
            )

            ReminderTypeItem(
                title = "Власний інтервал",
                subtitle = "Вказати хвилини вручну",
                isSelected = selectedType == ReminderType.CUSTOM_DURATION,
                onClick = { onTypeSelected(ReminderType.CUSTOM_DURATION) },
            )

            ReminderTypeItem(
                title = "Конкретна дата і час",
                subtitle = "Вибрати точний момент",
                isSelected = selectedType == ReminderType.SPECIFIC_DATETIME,
                onClick = { onTypeSelected(ReminderType.SPECIFIC_DATETIME) },
            )
        }
    }
}

@Composable
private fun ReminderTypeItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            ).background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ).clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
