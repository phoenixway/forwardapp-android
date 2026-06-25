package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun BottomPanelActionRow(
    modifier: Modifier = Modifier,
    leadingContent: @Composable RowScope.() -> Unit,
    trailingContent: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = BottomPanelTokens.ContentHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(BottomPanelTokens.RowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent()
        Spacer(modifier = Modifier.weight(1f))
        trailingContent()
    }
}
