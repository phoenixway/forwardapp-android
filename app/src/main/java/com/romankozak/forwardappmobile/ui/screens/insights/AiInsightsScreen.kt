

package com.romankozak.forwardappmobile.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.UUID


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
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
)


val aiMessages =
    listOf(
        AiMessage(
            text = "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            type = MessageType.MOTIVATION,
        ),
        AiMessage(text = "Why did the scarecrow win an award? Because he was outstanding in his field.", type = MessageType.JOKE),
        AiMessage(text = "This is an informational message.", type = MessageType.INFO),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInsightsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Insights") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            
            items(aiMessages) { message ->
                AiMessageCard(message = message)
            }
        }
    }
}

@Composable
fun AiMessageCard(message: AiMessage) {
    
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (message.type == MessageType.MOTIVATION) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Motivation",
                    tint = textColor,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
            
            Text(
                text = message.text,
                color = textColor,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
