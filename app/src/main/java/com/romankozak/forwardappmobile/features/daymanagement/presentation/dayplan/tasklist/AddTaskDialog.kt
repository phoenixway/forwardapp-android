package com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.data.database.models.RecurrenceRule
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.components.AdvancedRecurrencePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
  onDismissRequest: () -> Unit,
  onConfirm:
    (
      title: String,
      description: String,
      duration: Long?,
      priority: TaskPriority,
      recurrenceRule: RecurrenceRule?,
      points: Int,
    ) -> Unit,
  initialPriority: TaskPriority = TaskPriority.MEDIUM,
) {
  // STATE
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var durationText by remember { mutableStateOf("") }
  var pointsText by remember { mutableStateOf("") }
  var priority by remember { mutableStateOf(initialPriority) }
  var recurrenceRule by remember { mutableStateOf<RecurrenceRule?>(null) }
  var showRecurrencePicker by remember { mutableStateOf(false) }

  if (showRecurrencePicker) {
    AdvancedRecurrencePickerDialog(
      onDismiss = { showRecurrencePicker = false },
      onConfirm = { rule ->
        recurrenceRule = rule
        showRecurrencePicker = false
      },
    )
  }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.padding(24.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
      ) {
        Box(
          modifier =
            Modifier.size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                      )
                  )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
          )
        }
        Spacer(Modifier.width(16.dp))
        Text(
          text = "Нове завдання",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    },
    text = {
      Column(
        modifier =
          Modifier
            .padding(top = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {

        //-----------------------------------------------------------------
        // Title Field
        //-----------------------------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Назва",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
          )
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Введіть назву завдання") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              ),
            isError = title.isBlank(),
            supportingText = {
              if (title.isBlank()) {
                Text("Обов'язкове поле", color = MaterialTheme.colorScheme.error)
              }
            },
          )
        }

        //-----------------------------------------------------------------
        // Description Field
        //-----------------------------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Опис",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
          )
          OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Додаткова інформація (необов'язково)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
            maxLines = 4,
            shape = RoundedCornerShape(12.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              ),
          )
        }

        //-----------------------------------------------------------------
        // Points + Duration
        //-----------------------------------------------------------------
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Бали",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
              fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
              value = pointsText,
              onValueChange = {
                if (it.all { ch -> ch.isDigit() } || it.isEmpty()) pointsText = it
              },
              placeholder = { Text("0") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              leadingIcon = {
                Icon(
                  Icons.Default.Star,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              },
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Тривалість",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
              fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
              value = durationText,
              onValueChange = {
                if (it.all { ch -> ch.isDigit() } || it.isEmpty()) durationText = it
              },
              placeholder = { Text("0") },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              leadingIcon = {
                Icon(
                  Icons.Default.AccessTime,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                )
              },
              suffix = { if (durationText.isNotBlank()) Text("хв") },
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
          }
        }

        //-----------------------------------------------------------------
        // Recurrence
        //-----------------------------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Повторення",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
          )
          Surface(
            modifier = Modifier.fillMaxWidth().clickable { showRecurrencePicker = true },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            color = MaterialTheme.colorScheme.surface,
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  Icons.Default.Repeat,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = recurrenceRule?.let { it.frequency.name } ?: "Не повторюється",
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
              Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
              )
            }
          }
        }

        //-----------------------------------------------------------------
        // Priority Selection
        //-----------------------------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Пріоритет",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
          )

          FlowRow(mainAxisSpacing = 10.dp, crossAxisSpacing = 10.dp) {
            TaskPriority.values().forEach { prio ->
              val selected = priority == prio
              val color =
                when (prio) {
                  TaskPriority.LOW -> Color(0xFF4CAF50)
                  TaskPriority.MEDIUM -> Color(0xFF2196F3)
                  TaskPriority.HIGH -> Color(0xFFFF9800)
                  TaskPriority.CRITICAL -> Color(0xFFF44336)
                  TaskPriority.NONE -> Color.Gray
                }

              FilterChip(
                selected = selected,
                onClick = { priority = prio },
                label = {
                  Text(
                    prio.name,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                  )
                },
                enabled = true,
                leadingIcon =
                  if (selected) {
                    {
                      Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  } else null,
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.15f),
                    selectedLabelColor = color,
                    selectedLeadingIconColor = color,
                  ),
                border =
                  if (selected) {
                    FilterChipDefaults.filterChipBorder(
                      enabled = true,
                      selected = true,
                      borderColor = color,
                      selectedBorderColor = color,
                      borderWidth = 1.5.dp,
                      selectedBorderWidth = 1.5.dp,
                    )
                  } else {
                    FilterChipDefaults.filterChipBorder(
                      enabled = true,
                      selected = false,
                      borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                  },
              )
            }
          }
        }
      }
    },

    //---------------------------------------------------------------------
    // Action Buttons
    //---------------------------------------------------------------------
    confirmButton = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Text(
                    "Скасувати",
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    val duration = durationText.toLongOrNull()
                    val points = pointsText.toIntOrNull() ?: 0
                    onConfirm(title, description, duration, priority, recurrenceRule, points)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Додати", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    },
    dismissButton = {}
  )
}