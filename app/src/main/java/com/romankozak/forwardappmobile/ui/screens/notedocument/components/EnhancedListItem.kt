package com.romankozak.forwardappmobile.ui.screens.notedocument.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
private fun EnhancedListItem(
  text: String,
  indent: Int,
  isParent: Boolean,
  isCollapsed: Boolean,
  lineIndex: Int,
  onClick: (() -> Unit)?,
) {
  val animatedElevation by
    animateDpAsState(
      targetValue = if (isParent) 4.dp else 2.dp,
      animationSpec = tween(200),
      label = "elevation",
    )

  val animatedBackgroundColor by
    animateColorAsState(
      targetValue =
        when {
          isParent && isCollapsed -> MaterialTheme.colorScheme.tertiaryContainer
          isParent -> MaterialTheme.colorScheme.secondaryContainer
          else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
      animationSpec = tween(200),
      label = "background",
    )

  Card(
    modifier =
      Modifier.fillMaxWidth().padding(start = (indent * 16).dp).let { modifier ->
        if (onClick != null) {
          modifier.clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
          )
        } else {
          modifier
        }
      },
    colors = CardDefaults.cardColors(containerColor = animatedBackgroundColor),
    elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
    border =
      if (isParent) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      else null,
    shape =
      RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = if (isCollapsed) 12.dp else 8.dp,
        bottomEnd = if (isCollapsed) 12.dp else 8.dp,
      ),
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = if (isParent) 16.dp else 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Індикатор типу елементу
      if (isParent) {
        val icon =
          if (isCollapsed) {
            Icons.Default.ChevronRight
          } else {
            Icons.Default.KeyboardArrowDown
          }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
          modifier = Modifier.size(32.dp),
        ) {
          Icon(
            imageVector = icon,
            contentDescription = if (isCollapsed) "Розгорнути" else "Згорнути",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize().padding(6.dp),
          )
        }
      } else {
        // Маркер для звичайних елементів
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
          when {
            text.startsWith("• ") -> {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(8.dp),
              ) {}
            }
            text.matches(Regex("^\\d+\\..*")) -> {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                    text = text.takeWhile { it.isDigit() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
            text.startsWith("☐ ") -> {
              Icon(
                imageVector = Icons.Default.CheckBoxOutlineBlank,
                contentDescription = "Не виконано",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
              )
            }
            text.startsWith("☑ ") -> {
              Icon(
                imageVector = Icons.Default.CheckBox,
                contentDescription = "Виконано",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
            else -> {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(6.dp),
              ) {}
            }
          }
        }
      }

      // Текст елементу
      val displayText =
        when {
          text.startsWith("• ") -> text.removePrefix("• ")
          text.startsWith("☐ ") -> text.removePrefix("☐ ")
          text.startsWith("☑ ") -> text.removePrefix("☑ ")
          text.matches(Regex("^\\d+\\.\\s.*")) -> text.replaceFirst(Regex("^\\d+\\.\\s"), "")
          else -> text
        }

      Text(
        text = displayText,
        style =
          when {
            isParent -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.bodyLarge
          },
        color =
          when {
            isParent && isCollapsed -> MaterialTheme.colorScheme.onTertiaryContainer
            isParent -> MaterialTheme.colorScheme.onSecondaryContainer
            text.startsWith("☑ ") -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
        fontWeight = if (isParent) FontWeight.SemiBold else FontWeight.Normal,
        textDecoration = if (text.startsWith("☑ ")) TextDecoration.LineThrough else null,
        modifier = Modifier.weight(1f),
      )

      // Додаткові індикатори
      if (isCollapsed) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ) {
          Text(
            text = "...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}
