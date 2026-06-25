package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout

@Composable
fun BottomPanelSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = bottomPanelColors()
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
            color = colors.container,
            contentColor = colors.content,
            border =
                BorderStroke(
                    width = BottomPanelTokens.BorderWidth,
                    color = colors.border,
                ),
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
