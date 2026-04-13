package com.romankozak.forwardappmobile.desktop.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DesktopDashboardScreen() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DashboardHeroCard(
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DashboardSummaryCard(
                title = "Migration Layer",
                body = "Desktop shell запущений. Наступний крок: shared contracts, desktop state store, persistence adapters.",
            )
            DashboardSummaryCard(
                title = "Vertical Slice",
                body = "Перший slice для desktop має бути Context Explorer + Backlog Reader з read-only data contract.",
            )
        }
    }
}

@Composable
private fun DashboardHeroCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF143F40), Color(0xFF315C55), Color(0xFFBA8E44)),
                        ),
                    )
                    .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ForwardApp Desktop",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFFF7F3EC),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Не порт Android-екранів 1:1, а окремий desktop shell з чистими модулями, явними boundaries і міграцією shared domain.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE3E0D6),
                )
            }
            Text(
                text = "Stage 0: shell + architecture boundary",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFDF2D8),
            )
        }
    }
}

@Composable
private fun DashboardSummaryCard(
    title: String,
    body: String,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
