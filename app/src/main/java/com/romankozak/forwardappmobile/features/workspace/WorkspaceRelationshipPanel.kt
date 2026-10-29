package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.navigation.NavTarget

/** Semantic relationship diagram, not an editable structural hierarchy. All colors follow the theme. */
@Composable
internal fun WorkspaceRelationshipPanel(projection: WorkspaceRelationsProjection, workspaceTitle: String, onOpen: (NavTarget) -> Unit) {
    val model = projection.relationshipPanel()
    val rail = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    Column(Modifier.fillMaxWidth()) {
        model.source?.let { source ->
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SourceTile(workspaceTitle, "Workspace", "WORKSPACE", Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Embodies", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        SourceTile(source.title, source.typeLabel, source.targetType, Modifier.weight(1f),
                            onClick = source.navigation?.let { destination -> { onOpen(destination) } })
                    }
                    source.kinds.filterNot { it == "EMBODIES" }.takeIf { it.isNotEmpty() }?.let { kinds ->
                        Text(kinds.joinToString(" · "), Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (model.groups.isNotEmpty()) Spacer(Modifier.fillMaxWidth().height(30.dp).drawBehind {
                val x = size.width * 0.75f
                val end = 12.dp.toPx()
                val middle = size.height / 2
                val path = Path().apply {
                    moveTo(x, 0f); quadraticTo(x, middle, x - 16.dp.toPx(), middle)
                    lineTo(end + 12.dp.toPx(), middle); quadraticTo(end, middle, end, size.height)
                }
                drawPath(path, rail, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            })
        }
        if (model.groups.isNotEmpty()) Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(32.dp).height(36.dp).drawBehind {
                drawLine(rail, Offset(12.dp.toPx(), 0f), Offset(12.dp.toPx(), size.height), 1.5.dp.toPx())
                drawCircle(rail, 4.dp.toPx(), Offset(12.dp.toPx(), size.height / 2))
            })
            Text(model.structureLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        model.groups.forEachIndexed { index, group ->
            RelationshipBranch(group, index == model.groups.lastIndex, projection.embodiedOrientation?.title, onOpen)
        }
    }
}

@Composable
private fun SourceTile(title: String, type: String, targetType: String, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EntityBadge(targetType, small = true)
                Text(type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp))
            }
        }
    }
    val color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (onClick == null) 0.25f else 0.42f)
    if (onClick != null) Surface(onClick = onClick, modifier = modifier.semantics { contentDescription = "$type: $title. Відкрити семантичний редактор" },
        shape = RoundedCornerShape(16.dp), color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)), content = content)
    else Surface(modifier, shape = RoundedCornerShape(16.dp), color = color, content = content)
}

@Composable
private fun RelationshipBranch(group: WorkspaceEntityHubGroup, last: Boolean, orientationTitle: String?, onOpen: (NavTarget) -> Unit) {
    val rail = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().drawBehind {
            val x = 12.dp.toPx(); val middle = size.height / 2
            drawLine(rail, Offset(x, 0f), Offset(x, size.height), 1.5.dp.toPx())
            drawLine(rail, Offset(x, middle), Offset(32.dp.toPx(), middle), 1.5.dp.toPx())
            drawCircle(rail, 4.dp.toPx(), Offset(x, middle))
        }.padding(start = 32.dp, top = 10.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(entityIcon(group.rows.first().targetType), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(group.label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("(${group.rows.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        group.rows.forEachIndexed { index, row ->
            Box(Modifier.fillMaxWidth().drawBehind {
                val x = 12.dp.toPx(); val y = (size.height - 10.dp.toPx()) / 2
                val end = if (last && index == group.rows.lastIndex) y else size.height
                drawLine(rail, Offset(x, 0f), Offset(x, end), 1.5.dp.toPx())
                drawLine(rail, Offset(x, y), Offset(32.dp.toPx(), y), 1.5.dp.toPx())
                drawCircle(rail, 3.5.dp.toPx(), Offset(x, y))
            }.padding(start = 32.dp, bottom = 10.dp)) { RelationshipEntityCard(row, orientationTitle, onOpen) }
        }
        Spacer(Modifier.fillMaxWidth().height(14.dp).drawBehind {
            if (!last) drawLine(rail, Offset(12.dp.toPx(), 0f), Offset(12.dp.toPx(), size.height), 1.5.dp.toPx())
        })
    }
}

@Composable
private fun RelationshipEntityCard(row: WorkspaceRelationItem, orientationTitle: String?, onOpen: (NavTarget) -> Unit) {
    val relationLabel = row.panelRelationLabel(orientationTitle)
    Surface(onClick = { row.navigation?.let(onOpen) }, enabled = row.navigation != null,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${row.title}. ${row.typeLabel}. $relationLabel" },
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))) {
        Row(Modifier.heightIn(min = 80.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntityBadge(row.targetType)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                    Text(row.typeLabel, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(relationLabel + if (row.navigation == null) " · Перегляд недоступний" else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (row.navigation != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EntityBadge(type: String, small: Boolean = false) {
    Box(Modifier.size(if (small) 28.dp else 44.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center) {
        Icon(entityIcon(type), null, Modifier.size(if (small) 18.dp else 26.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

private fun entityIcon(type: String): ImageVector = when (type) {
    "ORIENTATION" -> Icons.Outlined.Adjust
    "ASPECT" -> Icons.Outlined.Layers
    "WORKSPACE" -> Icons.Outlined.WorkOutline
    else -> Icons.Outlined.Description
}
