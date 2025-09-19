/*
package com.romankozak.forwardappmobile.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.navigation.NavigationEntry
import com.romankozak.forwardappmobile.ui.navigation.NavigationType
import java.text.SimpleDateFormat
import java.util.*

*/
/**
 * Компонент для відображення кнопок навігації в TopBar
 *//*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationControls(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackPressed: () -> Unit,
    onForwardPressed: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        // Кнопка назад
        AnimatedVisibility(
            visible = canGoBack,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

// К*/
