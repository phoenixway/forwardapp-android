package com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.GoalList

@Composable
fun ListTitleBar(
    goalList: GoalList?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = goalList?.name ?: stringResource(id = R.string.loading),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp), // Більше відступ, щоб уникнути накладання
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Показуємо індикатор статусу, тільки якщо увімкнено управління проектом
            AnimatedVisibility(visible = goalList?.isProjectManagementEnabled == true && goalList.projectStatus != null) {
                if (goalList?.projectStatus != null) {
                    ProjectStatusIndicator(
                        status = goalList.projectStatus,
                        statusText = goalList.projectStatusText
                    )
                }
            }
        }
    }
}
