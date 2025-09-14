package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun DayAnalyticsScreen(navController: NavController) {
    // Цей екран буде показувати глибоку аналітику, тренди та інсайти.
    // Поки що це заглушка.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Екран Аналітики та Інсайтів")
    }
}
