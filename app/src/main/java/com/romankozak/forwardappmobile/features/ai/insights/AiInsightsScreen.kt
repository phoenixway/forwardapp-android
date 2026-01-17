package com.romankozak.forwardappmobile.features.ai.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.AiInsightEntity
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.features.ai.data.repository.AiInsightRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max


enum class MessageType {
    MOTIVATION,
    JOKE,
    INFO,
    WARNING,
    ERROR,
}


data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: MessageType,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
)

@HiltViewModel
class AiInsightsViewModel @Inject constructor(
    activityRepository: ActivityRepository,
    private val aiInsightRepository: AiInsightRepository,
) : ViewModel() {

    val messages: StateFlow<List<AiMessage>> =
        aiInsightRepository.observeInsights()
            .map { entities -> entities.map { it.toUi() } }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            activityRepository.getLogStream()
                .map { records -> buildInsights(records) }
                .collect { generated ->
                    aiInsightRepository.upsertInsights(generated)
                }
        }
    }

    private fun buildInsights(records: List<ActivityRecord>): List<AiInsightEntity> {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(now)
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000
        val fiveHoursAgo = now - 5 * 60 * 60 * 1000

        val todayRecords = records.filter { it.createdAt in todayStart until (todayStart + 24 * 60 * 60 * 1000) }
        val yesterdayRecords = records.filter { it.createdAt in yesterdayStart until todayStart }
        val lastFiveHours = records.filter { it.createdAt >= fiveHoursAgo }
        val lastDay = records.filter { it.createdAt >= yesterdayStart }

        val messages = mutableListOf<AiInsightEntity>()

        if (todayRecords.isEmpty()) {
            messages.add(
                AiInsightEntity(
                    id = "today_no_activity",
                    text = "Сьогодні ще не було активностей. Заплануй або відслідкуй невелику дію, щоб розігрітися.",
                    type = MessageType.WARNING.name,
                    timestamp = now,
                    isRead = false,
                )
            )
        }

        fun durationMinutes(record: ActivityRecord): Long {
            return record.durationInMillis?.let { max(1L, it / 60_000) } ?: 1L
        }

        fun buildFocusMessage(
            windowId: String,
            windowLabel: String,
            windowRecords: List<ActivityRecord>,
            minMinutes: Long,
        ) {
            val grouped = windowRecords
                .filter { it.projectId != null }
                .groupBy { it.projectId!! }
                .mapValues { entry -> entry.value.sumOf { durationMinutes(it) } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .filter { it.second >= minMinutes }

            grouped.forEachIndexed { index, (projectId, minutes) ->
                messages.add(
                    AiInsightEntity(
                        id = "${windowId}_${projectId}_$index",
                        text = "Фокус за $windowLabel: проєкт $projectId ~ ${minutes} хв.",
                        type = MessageType.INFO.name,
                        timestamp = now,
                        isRead = false,
                    )
                )
            }
        }

        buildFocusMessage("focus_5h", "останні 5 год", lastFiveHours, minMinutes = 20)
        buildFocusMessage("focus_24h", "добу", lastDay, minMinutes = 60)

        val yesterdayXp = yesterdayRecords.sumOf { it.xpGained ?: 0 }
        val yesterdayAnti = yesterdayRecords.sumOf { it.antyXp ?: 0 }
        if (yesterdayRecords.isNotEmpty() && (yesterdayXp + yesterdayAnti) <= 3) {
            messages.add(
                AiInsightEntity(
                    id = "yesterday_low_activity",
                    text = "Вчора було мало руху (+$yesterdayXp / -$yesterdayAnti). Спробуй запланувати один сфокусований блок сьогодні.",
                    type = MessageType.INFO.name,
                    timestamp = now,
                    isRead = false,
                )
            )
        }

        if (messages.isEmpty()) {
            messages.add(
                AiInsightEntity(
                    id = "keep_it_up",
                    text = "Продовжуй у тому ж дусі! Якщо хочеш, додай маленьку дію для підтримки ритму.",
                    type = MessageType.MOTIVATION.name,
                    timestamp = now,
                    isRead = false,
                )
            )
        }

        return messages
    }

    private fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun markRead(id: String) {
        viewModelScope.launch { aiInsightRepository.markRead(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { aiInsightRepository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { aiInsightRepository.clearAll() }
    }

    private fun AiInsightEntity.toUi(): AiMessage =
        AiMessage(
            id = id,
            text = text,
            type = MessageType.valueOf(type),
            timestamp = timestamp,
            isRead = isRead,
            isFavorite = isFavorite,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInsightsScreen(navController: NavController, viewModel: AiInsightsViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Insights") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAll() }) { Text("Очистити") }
                    }
                }
            )
        },
    ) { paddingValues ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Поки немає інсайтів", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                
                items(messages) { message ->
                    AiMessageCard(
                        message = message,
                        onMarkRead = { viewModel.markRead(message.id) },
                        onDelete = { viewModel.delete(message.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun AiMessageCard(
    message: AiMessage,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
) {
    
    val backgroundColor =
        when (message.type) {
            MessageType.MOTIVATION -> MaterialTheme.colorScheme.primaryContainer
            MessageType.JOKE -> MaterialTheme.colorScheme.secondaryContainer
            MessageType.INFO -> MaterialTheme.colorScheme.tertiaryContainer
            MessageType.WARNING -> MaterialTheme.colorScheme.errorContainer
            MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
        }
    val textColor =
        when (message.type) {
            MessageType.MOTIVATION -> MaterialTheme.colorScheme.onPrimaryContainer
            MessageType.JOKE -> MaterialTheme.colorScheme.onSecondaryContainer
            MessageType.INFO -> MaterialTheme.colorScheme.onTertiaryContainer
            MessageType.WARNING -> MaterialTheme.colorScheme.onErrorContainer
            MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        }

    val formatter = rememberDateFormatter()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.type == MessageType.MOTIVATION) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Motivation",
                        tint = textColor,
                    )
                }
                Text(
                    text = formatter.format(Date(message.timestamp)),
                    color = textColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                if (!message.isRead) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Unread",
                        tint = textColor,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Видалити", tint = textColor)
                }
            }
            Text(
                text = message.text,
                color = textColor,
                fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
            )
            if (!message.isRead) {
                TextButton(onClick = onMarkRead) { Text("Позначити прочитаним") }
            }
        }
    }
}

@Composable
private fun rememberDateFormatter(): SimpleDateFormat =
    remember {
        SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    }
