package com.romankozak.forwardappmobile.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatQuote
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

enum class MessageType {
    MOTIVATION,
    JOKE
}

data class AiMessage(val text: String, val type: MessageType)

val aiMessages = listOf(
    AiMessage("Success is not final, failure is not fatal: it is the courage to continue that counts.", MessageType.MOTIVATION),
    AiMessage("The only place where success comes before work is in the dictionary.", MessageType.MOTIVATION),
    AiMessage("Success is not the key to happiness. Happiness is the key to success. If you love what you are doing, you will be successful.", MessageType.MOTIVATION),
    AiMessage("The road to success and the road to failure are almost exactly the same.", MessageType.MOTIVATION),
    AiMessage("I find that the harder I work, the more luck I seem to have.", MessageType.MOTIVATION),
    AiMessage("Don't be afraid to give up the good to go for the great.", MessageType.MOTIVATION),
    AiMessage("Success usually comes to those who are too busy to be looking for it.", MessageType.MOTIVATION),
    AiMessage("If you are not willing to risk the usual, you will have to settle for the ordinary.", MessageType.MOTIVATION),
    AiMessage("The only limit to our realization of tomorrow will be our doubts of today.", MessageType.MOTIVATION),
    AiMessage("The way to get started is to quit talking and begin doing.", MessageType.MOTIVATION),
    AiMessage("Success is walking from failure to failure with no loss of enthusiasm.", MessageType.MOTIVATION),
    AiMessage("I owe my success to having listened respectfully to the very best advice, and then going away and doing the exact opposite.", MessageType.MOTIVATION),
    AiMessage("All our dreams can come true, if we have the courage to pursue them.", MessageType.MOTIVATION),
    AiMessage("The secret of success is to do the common thing uncommonly well.", MessageType.MOTIVATION),
    AiMessage("If you want to achieve excellence, you can get there today. As of this second, quit doing less-than-excellent work.", MessageType.MOTIVATION),
    AiMessage("Patience, persistence and perspiration make an unbeatable combination for success.", MessageType.MOTIVATION),
    AiMessage("The successful warrior is the average man, with laser-like focus.", MessageType.MOTIVATION),
    AiMessage("There are no secrets to success. It is the result of preparation, hard work, and learning from failure.", MessageType.MOTIVATION),
    AiMessage("Would you like me to give you a formula for success? It's quite simple, really: Double your rate of failure. You are thinking of failure as the enemy of success. But it isn't at all. You can be discouraged by failure or you can learn from it, so go ahead and make mistakes. Make all you can. Because remember that's where you'll find success.", MessageType.MOTIVATION),
    AiMessage("Success is not about how much money you make, it's about the difference you make in people's lives.", MessageType.MOTIVATION),
    AiMessage("The difference between a successful person and others is not a lack of strength, not a lack of knowledge, but rather a lack of will.", MessageType.MOTIVATION),
    AiMessage("Action is the foundational key to all success.", MessageType.MOTIVATION),
    AiMessage("Success seems to be connected with action. Successful people keep moving. They make mistakes, but they don't quit.", MessageType.MOTIVATION),
    AiMessage("The price of success is hard work, dedication to the job at hand, and the determination that whether we win or lose, we have applied the best of ourselves to the task at hand.", MessageType.MOTIVATION),
    AiMessage("Some people dream of success, while others wake up and work hard at it.", MessageType.MOTIVATION),
    AiMessage("Your most unhappy customers are your greatest source of learning.", MessageType.MOTIVATION),
    AiMessage("Try not to become a man of success, but rather try to become a man of value.", MessageType.MOTIVATION),
    AiMessage("Success is the sum of small efforts, repeated day-in and day-out.", MessageType.MOTIVATION),
    AiMessage("Develop success from failures. Discouragement and failure are two of the surest stepping stones to success.", MessageType.MOTIVATION),
    AiMessage("The starting point of all achievement is desire.", MessageType.MOTIVATION),
    AiMessage("Why did the productivity guru break up with the calendar? It had too many commitments.", MessageType.JOKE),
    AiMessage("My to-do list is so long, it's now a \"to-don't\" list.", MessageType.JOKE),
    AiMessage("I'm so good at multitasking, I can waste time, be unproductive, and procrastinate all at once.", MessageType.JOKE),
    AiMessage("Success is 1% inspiration, 99% perspiration, and 100% avoiding distractions.", MessageType.JOKE),
    AiMessage("My boss asked me to be more productive. So I started taking longer coffee breaks to \"think.\"", MessageType.JOKE),
    AiMessage("What's a procrastinator's favorite exercise? Jumping to conclusions.", MessageType.JOKE),
    AiMessage("I've mastered the art of productive procrastination. I clean my entire house instead of doing my actual work.", MessageType.JOKE),
    AiMessage("The early bird gets the worm, but the second mouse gets the cheese.", MessageType.JOKE),
    AiMessage("My goal is to be so productive that my computer asks me for a break.", MessageType.JOKE),
    AiMessage("I tried to be productive today, but then I remembered I had a couch.", MessageType.JOKE),
    AiMessage("Why did the successful entrepreneur bring a ladder to the meeting? To reach new heights!", MessageType.JOKE),
    AiMessage("My productivity hack is to pretend I only have 5 minutes to do everything. It works until I check the clock.", MessageType.JOKE),
    AiMessage("I'm not lazy, I'm just on energy-saving mode.", MessageType.JOKE),
    AiMessage("What do you call a successful person who never gives up? Tired.", MessageType.JOKE),
    AiMessage("My therapist told me to embrace my flaws. So now I'm embracing my procrastination.", MessageType.JOKE),
    AiMessage("I'm so productive, I even procrastinate on my procrastination.", MessageType.JOKE),
    AiMessage("The only thing I've successfully completed today is a full cycle of laundry. And I'm still wearing pajamas.", MessageType.JOKE),
    AiMessage("Why don't secrets last long in a productive office? Because everyone is always *working* on them!", MessageType.JOKE),
    AiMessage("I have a black belt in procrastination. I'm a master of delaying.", MessageType.JOKE),
    AiMessage("Success is like a ladder. You can't climb it with your hands in your pockets.", MessageType.JOKE),
    AiMessage("My brain has two settings: \"on fire\" and \"on vacation.\" There's no in-between for productivity.", MessageType.JOKE),
    AiMessage("I'm not avoiding work; I'm just giving it time to mature.", MessageType.JOKE),
    AiMessage("What's the secret to success? Start before you're ready.", MessageType.JOKE),
    AiMessage("I'm trying to be more productive, but my bed keeps calling my name.", MessageType.JOKE),
    AiMessage("My productivity level is directly proportional to how close the deadline is.", MessageType.JOKE),
    AiMessage("Why did the successful person get a dog? Because they needed someone to chase their dreams with.", MessageType.JOKE),
    AiMessage("I'm so good at prioritizing, I can put off important tasks for even more important naps.", MessageType.JOKE),
    AiMessage("The only thing standing between me and success is me. And maybe a snack.", MessageType.JOKE),
    AiMessage("My productivity app just sent me a notification: \"Are you still there?\"", MessageType.JOKE),
    AiMessage("I've achieved peak productivity. I've successfully avoided all work for the entire day.", MessageType.JOKE)
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
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(aiMessages) { message ->
                AiMessageCard(message = message)
            }
        }
    }
}

@Composable
fun AiMessageCard(message: AiMessage) {
    val icon = when (message.type) {
        MessageType.MOTIVATION -> Icons.Default.Lightbulb
        MessageType.JOKE -> Icons.Default.FormatQuote
    }
    val backgroundColor = when (message.type) {
        MessageType.MOTIVATION -> MaterialTheme.colorScheme.primaryContainer
        MessageType.JOKE -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when (message.type) {
        MessageType.MOTIVATION -> MaterialTheme.colorScheme.onPrimaryContainer
        MessageType.JOKE -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = message.text,
                color = textColor,
                fontWeight = FontWeight.Normal
            )
        }
    }
}