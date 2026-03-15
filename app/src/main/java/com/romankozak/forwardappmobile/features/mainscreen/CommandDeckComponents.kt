@file:Suppress("MatchingDeclarationName")

package com.romankozak.forwardappmobile.features.mainscreen

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

private const val CORE_TAB_ACCENT_COLOR_HEX = 0xFFBB86FC
private const val STRATEGY_TAB_ACCENT_COLOR_HEX = 0xFF4FC3F7
private const val STRATEGIC_ARC_TAB_ACCENT_COLOR_HEX = 0xFF9575CD
private const val TACTICS_TAB_ACCENT_COLOR_HEX = 0xFF26A69A
private const val TODAY_TAB_ACCENT_COLOR_HEX = 0xFFFFB74D
private const val DASHBOARD_TAB_ACCENT_COLOR_HEX = 0xFF6200EE
private val CoreTabAccentColor = Color(CORE_TAB_ACCENT_COLOR_HEX)
private val StrategyTabAccentColor = Color(STRATEGY_TAB_ACCENT_COLOR_HEX)
private val StrategicArcTabAccentColor = Color(STRATEGIC_ARC_TAB_ACCENT_COLOR_HEX)
private val TacticsTabAccentColor = Color(TACTICS_TAB_ACCENT_COLOR_HEX)
private val TodayTabAccentColor = Color(TODAY_TAB_ACCENT_COLOR_HEX)
private val DashboardTabAccentColor = Color(DASHBOARD_TAB_ACCENT_COLOR_HEX)
private const val TAB_GLOW_ANIMATION_MILLIS = 450
private const val TAB_SELECTED_SCALE = 1.05f
private const val TAB_SPRING_DAMPING_RATIO = 0.75f
private const val TAB_SPRING_STIFFNESS = 320f
private const val TAB_GLOW_ALPHA = 0.18f
private const val TAB_BASE_ALPHA = 0.10f
private const val TAB_SECONDARY_ALPHA = 0.03f
private const val TAB_SELECTED_BORDER_DP = 1.4f
private const val TAB_DEFAULT_BORDER_DP = 0.8f
private const val TAB_SELECTED_BORDER_ALPHA = 0.9f
private const val TAB_DEFAULT_BORDER_ALPHA = 0.45f
private const val TAB_SHAPE_DP = 26
private const val TAB_HORIZONTAL_PADDING_DP = 10
private const val TAB_VERTICAL_PADDING_DP = 6
private const val TAB_SPECIAL_FONT_SIZE_SP = 22
private const val TAB_DEFAULT_FONT_SIZE_SP = 18
private const val TAB_SPECIAL_CIRCLE_SIZE_DP = 32
private const val TAB_DEFAULT_CIRCLE_SIZE_DP = 28
private const val TAB_SELECTED_BACKGROUND_ALPHA = 0.22f
private const val TAB_DEFAULT_BACKGROUND_ALPHA = 0.12f
private const val TAB_SYMBOL_Y_OFFSET_DP = -2
private const val TAB_SELECTED_SPACER_DP = 6
private const val DECK_MODULE_BACKGROUND_COLOR_HEX = 0xFF1E1E1E
private const val DECK_MODULE_SUBTITLE_COLOR_HEX = 0xFFCCCCCC
private val DeckModuleBackgroundColor = Color(DECK_MODULE_BACKGROUND_COLOR_HEX)
private val DeckModuleSubtitleColor = Color(DECK_MODULE_SUBTITLE_COLOR_HEX)
private const val DECK_MODULE_BACKGROUND_ALPHA = 0.22f
private const val DECK_MODULE_SHAPE_DP = 22
private const val DECK_MODULE_CONTENT_PADDING_DP = 20
private const val DECK_MODULE_SPACING_DP = 10
private const val DECK_MODULE_TITLE_SIZE_SP = 20
private const val DECK_MODULE_SUBTITLE_SIZE_SP = 14
private const val PROGRESS_TRACK_ALPHA = 0.15f
private const val PROGRESS_BAR_HEIGHT_DP = 6
private const val PROGRESS_BAR_SHAPE_DP = 50

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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        itemsIndexed(tabs) { index, tab ->
            CommandDeckTabItem(
                tab = tab,
                isSelected = index == selectedTabIndex,
                onClick = { onTabSelected(index) },
            )
        }
    }
}

// TAB COLORS
fun tabAccentColor(tab: CommandDeckTab): Color {
    return when (tab) {
        CommandDeckTab.Core -> CoreTabAccentColor
        CommandDeckTab.Strategy -> StrategyTabAccentColor
        CommandDeckTab.StrategicArc -> StrategicArcTabAccentColor
        CommandDeckTab.Tactics -> TacticsTabAccentColor
        CommandDeckTab.Today -> TodayTabAccentColor
        CommandDeckTab.Dashboard -> DashboardTabAccentColor
    }
}

// TAB ITEM
@Composable
fun CommandDeckTabItem(
    tab: CommandDeckTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = tabAccentColor(tab)
    val tabMetrics = tabItemMetrics(tab)

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) TAB_GLOW_ALPHA else 0f,
        animationSpec = tween(TAB_GLOW_ANIMATION_MILLIS),
        label = "glow",
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) TAB_SELECTED_SCALE else 1f,
        animationSpec =
            spring(
                dampingRatio = TAB_SPRING_DAMPING_RATIO,
                stiffness = TAB_SPRING_STIFFNESS,
            ),
        label = "scale",
    )

    Row(
        modifier =
            modifier
                .scale(scale)
                .clip(RoundedCornerShape(TAB_SHAPE_DP.dp))
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                accent.copy(alpha = TAB_BASE_ALPHA + glowAlpha),
                                accent.copy(alpha = TAB_SECONDARY_ALPHA),
                            ),
                    ),
                )
                .border(
                    width =
                        if (isSelected) {
                            TAB_SELECTED_BORDER_DP.dp
                        } else {
                            TAB_DEFAULT_BORDER_DP.dp
                        },
                    color =
                        accent.copy(
                            alpha =
                                if (isSelected) {
                                    TAB_SELECTED_BORDER_ALPHA
                                } else {
                                    TAB_DEFAULT_BORDER_ALPHA
                                },
                        ),
                    shape = RoundedCornerShape(TAB_SHAPE_DP.dp),
                )
                .clickable { onClick() }
                .padding(
                    horizontal = TAB_HORIZONTAL_PADDING_DP.dp,
                    vertical = TAB_VERTICAL_PADDING_DP.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(tabMetrics.circleSize.dp)
                    .clip(CircleShape)
                    .background(
                        accent.copy(
                            alpha =
                                if (isSelected) {
                                    TAB_SELECTED_BACKGROUND_ALPHA
                                } else {
                                    TAB_DEFAULT_BACKGROUND_ALPHA
                                },
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tab.symbol,
                fontSize = tabMetrics.fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                modifier = tabMetrics.symbolModifier,
            )
        }

        if (isSelected) {
            Spacer(Modifier.width(TAB_SELECTED_SPACER_DP.dp))
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
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(DECK_MODULE_SHAPE_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = DeckModuleBackgroundColor.copy(alpha = DECK_MODULE_BACKGROUND_ALPHA),
            ),
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(DECK_MODULE_CONTENT_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(DECK_MODULE_SPACING_DP.dp),
        ) {
            Text(
                text = title,
                fontSize = DECK_MODULE_TITLE_SIZE_SP.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Text(
                text = subtitle,
                fontSize = DECK_MODULE_SUBTITLE_SIZE_SP.sp,
                color = DeckModuleSubtitleColor,
            )

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = PROGRESS_TRACK_ALPHA),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(PROGRESS_BAR_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(PROGRESS_BAR_SHAPE_DP.dp)),
                )
            }
        }
    }
}

private data class TabItemMetrics(
    val fontSize: Int,
    val circleSize: Int,
    val symbolModifier: Modifier,
)

private fun tabItemMetrics(tab: CommandDeckTab): TabItemMetrics {
    val isSpecialTab =
        tab == CommandDeckTab.StrategicArc ||
            tab == CommandDeckTab.Tactics ||
            tab == CommandDeckTab.Today
    val symbolModifier =
        if (tab == CommandDeckTab.Tactics || tab == CommandDeckTab.StrategicArc) {
            Modifier.offset(y = TAB_SYMBOL_Y_OFFSET_DP.dp)
        } else {
            Modifier
        }

    return TabItemMetrics(
        fontSize = if (isSpecialTab) TAB_SPECIAL_FONT_SIZE_SP else TAB_DEFAULT_FONT_SIZE_SP,
        circleSize = if (isSpecialTab) TAB_SPECIAL_CIRCLE_SIZE_DP else TAB_DEFAULT_CIRCLE_SIZE_DP,
        symbolModifier = symbolModifier,
    )
}
