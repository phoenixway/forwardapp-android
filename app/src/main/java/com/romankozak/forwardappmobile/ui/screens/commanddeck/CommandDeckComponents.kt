package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ENUM TABS
enum class CommandDeckTab(
    val title: String,
    val symbol: String,
) {
    Dashboard("Command Deck", "⌗"),
    Today("Today", "⌁"),
    Tactics("Tactics", "◎"),
    StrategicArc("Strategic Arc", "⟲"),
    Strategy("Strategy", "⌖"),
    Core("Core", "⌘"),
}



// TAB ROW
@Composable
fun CommandDeckTabRow(
    tabs: List<CommandDeckTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        itemsIndexed(tabs) { index, tab ->
            CommandDeckTabItem(
                tab = tab,
                isSelected = index == selectedTabIndex,
                onClick = { onTabSelected(index) }
            )
        }
    }
}


// TAB COLORS
fun tabAccentColor(tab: CommandDeckTab): Color {
    return when (tab) {
        CommandDeckTab.Core -> Color(0xFFBB86FC)
        CommandDeckTab.Strategy -> Color(0xFF4FC3F7)
        CommandDeckTab.StrategicArc -> Color(0xFF9575CD)
        CommandDeckTab.Tactics -> Color(0xFF26A69A)
        CommandDeckTab.Today -> Color(0xFFFFB74D)
        CommandDeckTab.Dashboard -> Color(0xFF6200EE)
    }
}

// TAB ITEM
@Composable
fun CommandDeckTabItem(
    tab: CommandDeckTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = tabAccentColor(tab)

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.18f else 0f,
        animationSpec = tween(450),
        label = "glow"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "scale"
    )

    val isSpecialTab = tab == CommandDeckTab.StrategicArc || tab == CommandDeckTab.Tactics || tab == CommandDeckTab.Today
    val symbolFontSize = if (isSpecialTab) 22.sp else 18.sp // Even larger font size
    val circleSize = if (isSpecialTab) 32.dp else 28.dp // Even larger circle for special tabs

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.10f + glowAlpha),
                        accent.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = if (isSelected) 1.4.dp else 0.8.dp,
                color = accent.copy(alpha = if (isSelected) 0.9f else 0.45f),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ------------------------
        // UNIFIED CIRCLE ICON AREA
        // ------------------------
        Box(
            modifier = Modifier
                .size(circleSize) // Use dynamic size
                .clip(CircleShape)
                .background(accent.copy(alpha = if (isSelected) 0.22f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.symbol,
                fontSize = symbolFontSize, // Use dynamic font size
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                modifier = if (tab == CommandDeckTab.Tactics || tab == CommandDeckTab.StrategicArc) {
                    Modifier.offset(y = (-2).dp) // Apply a small upward offset
                } else {
                    Modifier
                }
            )
        }

        if (isSelected) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = tab.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}



// MODULE CARD
@Composable
fun DeckModuleCard(
    title: String,
    subtitle: String,
    progress: Int?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.22f)
        ),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color(0xFFCCCCCC)
            )

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}
