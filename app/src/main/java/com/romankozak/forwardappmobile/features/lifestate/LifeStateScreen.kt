package com.romankozak.forwardappmobile.features.lifestate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.domain.lifestate.model.AiAnalysis
import com.romankozak.forwardappmobile.domain.lifestate.model.AiOpportunity
import com.romankozak.forwardappmobile.domain.lifestate.model.AiRecommendation
import com.romankozak.forwardappmobile.domain.lifestate.model.AiRisk
import com.romankozak.forwardappmobile.features.ai.chat.ChatMessage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeStateScreen(
    navController: NavController,
    viewModel: LifeStateViewModel = hiltViewModel(),
    chatViewModel: LifeStateChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Life State Analysis") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalysis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                            ),
                    )
                    .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(
                    message = uiState.error ?: "An error occurred",
                    onRetry = { viewModel.loadAnalysis() },
                )
                uiState.analysis != null -> {
                    LaunchedEffect(uiState.analysis) {
                        uiState.analysis?.let { chatViewModel.attachContext(it) }
                    }
                    AnalysisContent(
                        analysis = uiState.analysis!!,
                        onRegenerateAnalysis = { viewModel.loadAnalysis(force = true) },
                        onBackgroundAnalysis = { viewModel.enqueueBackgroundAnalysis() },
                        chatSection = {
                            ChatSection(
                                state = chatState,
                                onInputChange = chatViewModel::onInputChange,
                                onSend = { chatViewModel.sendMessage(uiState.analysis!!) },
                                onRegenerate = { chatViewModel.regenerate(uiState.analysis!!) },
                                onRegenerateMessage = { msg ->
                                    chatViewModel.regenerateFromMessage(msg, uiState.analysis!!)
                                },
                                onQuickPrompt = { prompt ->
                                    chatViewModel.sendQuickPrompt(prompt, uiState.analysis!!)
                                },
                            )
                        },
                    )
                }
                else -> Text("No data available", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun AiAnalysis.toMarkdown(): String =
    buildString {
        appendLine("# Life State Analysis")
        appendLine()
        appendLine("## Summary")
        appendLine(summary)
        appendLine()
        if (keyProcesses.isNotEmpty()) {
            appendLine("## Key Processes")
            keyProcesses.forEach { appendLine("- $it") }
            appendLine()
        }
        appendLine("## Signals")
        if (signals.positive.isNotEmpty()) {
            appendLine("- Positive:")
            signals.positive.forEach { appendLine("  - $it") }
        }
        if (signals.negative.isNotEmpty()) {
            appendLine("- Negative:")
            signals.negative.forEach { appendLine("  - $it") }
        }
        appendLine()
        if (risks.isNotEmpty()) {
            appendLine("## Risks")
            risks.forEach { appendLine("- ${it.name}: ${it.description}") }
            appendLine()
        }
        if (opportunities.isNotEmpty()) {
            appendLine("## Opportunities")
            opportunities.forEach { appendLine("- ${it.name}: ${it.description}") }
            appendLine()
        }
        if (recommendations.isNotEmpty()) {
            appendLine("## Recommendations (24-48h)")
            recommendations.forEach { rec ->
                appendLine("- ${rec.title}: ${rec.message}")
                if (rec.actions.isNotEmpty()) {
                    rec.actions.forEach { act -> appendLine("  - [ ] ${act.label}") }
                }
            }
        }
    }

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text("Try again")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Preparing life analytics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        CircularProgressIndicator()
        Text(
            text = "Collecting activity and notes, crafting a summary…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) { SkeletonCard() }
        }
    }
}

@Composable
private fun SkeletonCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LinearProgressIndicator(
                progress = { 0.4f },
                modifier = Modifier.fillMaxWidth(0.6f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                trackColor = MaterialTheme.colorScheme.surface,
            )
            LinearProgressIndicator(
                progress = { 0.2f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                trackColor = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

@Composable
fun AnalysisContent(
    analysis: AiAnalysis,
    onRegenerateAnalysis: () -> Unit,
    onBackgroundAnalysis: () -> Unit,
    chatSection: (@Composable () -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { HighlightsHeader(analysis) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Latest analysis is cached",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TextButton(onClick = {
                        val md = analysis.toMarkdown()
                        clipboard.setText(AnnotatedString(md))
                    }) {
                        Text("Copy Markdown")
                    }
                    TextButton(onClick = onRegenerateAnalysis) {
                        Text("Regenerate analysis")
                    }
                    TextButton(onClick = onBackgroundAnalysis) {
                        Text("Run in background")
                    }
                }
            }
        }
        item {
            SectionCard(title = "Summary") {
                Text(
                    text = analysis.summary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (analysis.keyProcesses.isNotEmpty()) {
            item {
                SectionCard(title = "Key processes", collapsible = true, defaultExpanded = false) {
                    analysis.keyProcesses.forEach { process ->
                        Text("• $process", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            SectionCard(title = "Signals", collapsible = true, defaultExpanded = false) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SignalColumn(label = "Positive", items = analysis.signals.positive, accent = MaterialTheme.colorScheme.primary)
                    SignalColumn(label = "Negative", items = analysis.signals.negative, accent = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (analysis.risks.isNotEmpty()) {
            item {
                SectionCard(title = "Risks", collapsible = true, defaultExpanded = false) {
                    analysis.risks.forEach { risk ->
                        RiskRow(risk)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        if (analysis.opportunities.isNotEmpty()) {
            item {
                SectionCard(title = "Opportunities", collapsible = true, defaultExpanded = false) {
                    analysis.opportunities.forEach { opportunity ->
                        OpportunityRow(opportunity)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        if (analysis.recommendations.isNotEmpty()) {
            item {
                SectionCard(title = "Recommendations (24-48h)", collapsible = true, defaultExpanded = false) {
                    analysis.recommendations.forEach { recommendation ->
                        RecommendationCard(recommendation)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
        if (chatSection != null) {
            item {
                SectionCard(title = "Ask AI") {
                    chatSection()
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        } else {
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    collapsible: Boolean = false,
    defaultExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(defaultExpanded) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier =
                    if (collapsible) {
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                    } else {
                        Modifier.fillMaxWidth()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (collapsible) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                        )
                    }
                }
            }
            if (!collapsible || expanded) {
                content()
            }
        }
    }
}

@Composable
fun RowScope.SignalColumn(
    label: String,
    items: List<String>,
    accent: Color,
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
        if (items.isEmpty()) {
            Text("—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun RiskRow(risk: AiRisk) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = risk.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = risk.description,
            style = MaterialTheme.typography.bodyMedium,
        )
        AssistChip(
            onClick = {},
            label = { Text(text = "Severity: ${risk.severity}") },
        )
    }
}

@Composable
fun OpportunityRow(opportunity: AiOpportunity) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = opportunity.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = opportunity.description,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun RecommendationCard(recommendation: AiRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = recommendation.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            if (recommendation.actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    recommendation.actions.take(3).forEach { action ->
                        AssistChip(
                            onClick = {},
                            label = { Text(action.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightsHeader(analysis: AiAnalysis) {
    val positiveCount = analysis.signals.positive.size
    val negativeCount = analysis.signals.negative.size
    val risksCount = analysis.risks.size
    val oppCount = analysis.opportunities.size
    val recsCount = analysis.recommendations.size

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Life snapshot",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetricBadge("Positive", positiveCount, MaterialTheme.colorScheme.tertiary)
                MetricBadge("Negative", negativeCount, MaterialTheme.colorScheme.error)
                MetricBadge("Risks", risksCount, MaterialTheme.colorScheme.errorContainer)
                MetricBadge("Opportunities", oppCount, MaterialTheme.colorScheme.secondaryContainer)
                MetricBadge("Actions 24-48h", recsCount, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MetricBadge(
    label: String,
    value: Int,
    color: Color,
) {
    Column(
        modifier =
            Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color.copy(alpha = 0.95f),
        )
    }
}

@Composable
fun QuickPromptRow(
    prompts: List<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        prompts.forEach { prompt ->
            AssistChip(
                onClick = { if (enabled) onSelect(prompt) },
                enabled = enabled,
                label = { Text(prompt) },
            )
        }
    }
}

@Composable
fun ChatSection(
    state: LifeStateChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRegenerate: () -> Unit,
    onRegenerateMessage: (ChatMessage) -> Unit,
    onQuickPrompt: (String) -> Unit,
) {
    val isStreaming = state.messages.any { !it.isFromUser && it.isStreaming }
    val isBusy = state.isSending || isStreaming
    val chatListState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            chatListState.animateScrollToItem(maxOf(state.messages.size - 1, 0))
        }
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(min = 340.dp)
                .navigationBarsPadding()
                .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Ask AI",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        QuickPromptRow(
            prompts =
                listOf(
                    "Give me 3 steps for this week",
                    "How to reduce key risks now?",
                    "What can I delegate or postpone?",
                    "Plan for the next 48h",
                ),
            enabled = !isBusy,
            onSelect = onQuickPrompt,
        )
        if (state.isSending && !isStreaming) {
            SubtlePendingIndicator()
        }
        val history = state.messages.takeLast(20)
        if (history.isEmpty()) {
            Text(
                text = "Ask a question or request personal recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val isStreamingMessage = history.lastOrNull { !it.isFromUser }?.isStreaming == true
            LazyColumn(
                state = chatListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isStreamingMessage) {
                    item { StreamingChip() }
                }
                items(history.size) { index ->
                    val message = history[index]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ChatBubble(message = message)
                        if (message.isFromUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    if (message.isFromUser) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    enabled = !isBusy,
                                    onClick = { onRegenerateMessage(message) },
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Regenerate",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = state.userInput,
            onValueChange = { if (!isBusy) onInputChange(it) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp)),
            placeholder = { Text("Ask about next steps…") },
            singleLine = false,
            minLines = 1,
            maxLines = 4,
            trailingIcon = {
                IconButton(
                    onClick = onSend,
                    enabled = !isBusy && state.userInput.isNotBlank(),
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = "Send")
                }
            },
            enabled = !isBusy,
            shape = RoundedCornerShape(22.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.error != null) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    val background =
        if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val isStreaming = message.isStreaming && !isUser
    var animatedText by remember(message.id) { mutableStateOf(if (isUser) message.text else "") }

    LaunchedEffect(message.text, message.isStreaming) {
        if (!isStreaming) {
            animatedText = message.text
            return@LaunchedEffect
        }
        if (isUser) {
            animatedText = message.text
            return@LaunchedEffect
        }
        val target = message.text
        if (!target.startsWith(animatedText)) {
            animatedText = ""
        }
        var currentLength = animatedText.length
        while (currentLength < target.length) {
            currentLength += 1
            animatedText = target.take(currentLength)
            delay(12L)
        }
    }

    val displayText = animatedText.ifBlank { if (isStreaming) "…" else message.text }
    val avatarLabel = if (isUser) "You" else "AI"
    val avatarColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!isUser) {
                ChatAvatar(label = avatarLabel, color = avatarColor)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = background),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = avatarLabel,
                        color = contentColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = displayText,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (isStreaming) {
                        StreamingCursor(modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (isUser) {
                ChatAvatar(label = avatarLabel, color = avatarColor)
            }
        }
    }
}

@Composable
private fun ChatAvatar(label: String, color: Color) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(2).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun StreamingCursor(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cursorAlpha",
    )
    Text(
        text = "▌",
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun StreamingChip() {
    val transition = rememberInfiniteTransition(label = "streamingChip")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "chipAlpha",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp).graphicsLayer { this.alpha = alpha },
        )
        Text(
            text = "AI is typing…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
        )
    }
}

@Composable
fun SubtlePendingIndicator() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            text = "Preparing response…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
