package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.theme.InputModeColors
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier

@Composable
fun BottomPanelSurface(
    panelStyle: InputModeColors,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CommonBottomPanelLayout(modifier = modifier) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BottomPanelTokens.OuterHorizontalPadding,
                        vertical = BottomPanelTokens.OuterVerticalPadding,
                    ),
            shape = RoundedCornerShape(BottomPanelTokens.ContainerCornerRadius),
            color = panelStyle.backgroundColor,
            border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = BottomPanelTokens.ContainerHorizontalPadding,
                            vertical = BottomPanelTokens.ContainerVerticalPadding,
                        ),
                verticalArrangement = Arrangement.spacedBy(BottomPanelTokens.RowSpacing),
                content = content,
            )
        }
    }
}

@Composable
fun CommandDeckPanelSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CommonBottomPanelLayout(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BottomPanelTokens.OuterHorizontalPadding,
                        vertical = BottomPanelTokens.OuterVerticalPadding,
                    )
                    .clip(RoundedCornerShape(BottomPanelTokens.CommandDeckCornerRadius))
                    .then(CommandDeckBackgroundModifier())
                    .padding(
                        horizontal = BottomPanelTokens.CommandDeckHorizontalPadding,
                        vertical = BottomPanelTokens.CommandDeckVerticalPadding,
                    ),
        ) {
            content()
        }
    }
}
