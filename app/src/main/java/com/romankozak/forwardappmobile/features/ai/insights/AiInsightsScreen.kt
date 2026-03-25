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
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.ai.AiInsightEntity
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import com.romankozak.forwardappmobile.data.repository.UserAwarenessRepository
import com.romankozak.forwardappmobile.features.ai.data.repository.AiInsightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

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
class AiInsightsViewModel
    @Inject
    constructor(
        activityRepository: ActivityRepository,
        private val dayManagementRepository: DayManagementRepository,
        private val aiInsightRepository: AiInsightRepository,
        private val insightPolicyEngine: InsightPolicyEngine,
        userAwarenessRepository: UserAwarenessRepository,
        focusContextRepository: FocusContextRepository,
    ) : ViewModel() {
        val messages: StateFlow<List<AiMessage>> =
            aiInsightRepository.observeInsights()
                .map { entities -> entities.map { it.toUi() } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        init {
            viewModelScope.launch {
                combine(
                    activityRepository.getLogStream(),
                    userAwarenessRepository.observeActiveState(),
                    focusContextRepository.observeActiveFocusContextIds(),
                    tickerFlow(INSIGHTS_REEVALUATION_PERIOD_MS),
                ) { records, activeState, focusedContextIds, _ ->
                    insightPolicyEngine.evaluate(
                        records = records,
                        activeStateType = activeState.type,
                        hasFocusedContexts = focusedContextIds.isNotEmpty(),
                        planBaseline = ensureTodayPlanBaseline(startOfDay(System.currentTimeMillis())),
                    )
                }.collect { evaluation ->
                    val generated = evaluation.insights
                    val persistedById = aiInsightRepository.getAllSync().associateBy { it.id }
                    val merged =
                        generated.map { item ->
                            persistedById[item.id]?.let { persisted ->
                                insightPolicyEngine.mergeWithPersistedFlags(
                                    generated = item,
                                    persisted = persisted,
                                    now = System.currentTimeMillis(),
                                )
                            } ?: item
                        }
                    aiInsightRepository.upsertInsights(merged)
                    evaluation.staleInsightIds.forEach { staleId -> aiInsightRepository.deleteById(staleId) }
                }
            }
        }

        private fun tickerFlow(periodMillis: Long) =
            flow {
                emit(Unit)
                while (true) {
                    delay(periodMillis)
                    emit(Unit)
                }
            }

        private suspend fun ensureTodayPlanBaseline(todayStart: Long): PlanBaseline {
            val existingPlanId = dayManagementRepository.getPlanIdForDate(todayStart)
            val hadNoDayPlan = existingPlanId == null
            val existingTasks =
                if (existingPlanId != null) {
                    dayManagementRepository.getTasksForDayOnce(existingPlanId)
                } else {
                    emptyList()
                }

            val focusCount =
                existingTasks.count { task ->
                    task.title.contains(DAY_FOCUS_TAG, ignoreCase = true) ||
                        (task.description?.contains(DAY_FOCUS_TAG, ignoreCase = true) == true)
                }

            return PlanBaseline(
                hadNoDayPlan = hadNoDayPlan,
                focusCountBefore = focusCount,
            )
        }

        companion object {
            private const val DAY_FOCUS_TAG = "#day_focus"
            private const val INSIGHTS_REEVALUATION_PERIOD_MS = 30L * 60L * 1000L
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
            viewModelScope.launch { aiInsightRepository.deleteById(id) }
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
fun AiInsightsScreen(
    navController: NavController,
    viewModel: AiInsightsViewModel = hiltViewModel(),
) {
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
                },
            )
        },
    ) { paddingValues ->
        if (messages.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text("Поки немає інсайтів", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        } else {
            val groupedMessages =
                remember(messages) {
                    messages
                        .groupBy { it.timestamp.toDayStart() }
                        .toList()
                        .sortedByDescending { it.first }
                }
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                groupedMessages.forEach { (dayStart, dayMessages) ->
                    item(key = "day_header_$dayStart") {
                        DaySeparator(label = dayStart.toDayLabel())
                    }
                    items(dayMessages, key = { it.id }) { message ->
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

@Composable
private fun DaySeparator(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun Long.toDayStart(): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun Long.toDayLabel(): String {
    val dayStart = toDayStart()
    val todayStart = System.currentTimeMillis().toDayStart()
    val yesterdayStart = todayStart - 24L * 60L * 60L * 1000L
    return when (dayStart) {
        todayStart -> "Сьогодні"
        yesterdayStart -> "Вчора"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dayStart))
    }
}
