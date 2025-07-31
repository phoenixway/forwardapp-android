package com.romankozak.forwardappmobile.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.net.URLEncoder

/**
 * Composable, який рендерить текст з підтримкою Markdown та спеціального синтаксису.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    isCompleted: Boolean = false,
    obsidianVaultName: String = "",
    onTagClick: (String) -> Unit = {},
    onTextClick: () -> Unit = {} // ✨ ЗМІНА №1: Додано новий параметр для кліку по тексту
) {
    val context = LocalContext.current
    val tagColor = MaterialTheme.colorScheme.primary
    val projectColor = MaterialTheme.colorScheme.tertiary
    val linkColor = MaterialTheme.colorScheme.tertiary

    val finalTextStyle = if (isCompleted) {
        style.copy(
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    } else {
        style.copy(
            color = style.color.takeUnless { it.isUnspecified } ?: MaterialTheme.colorScheme.onSurface
        )
    }

    val listRegex = remember { Regex("^\\s*([*+-])\\s+(.*)") }
    val inlineContentRegex = remember {
        Regex(
            "(\\*\\*|__)(.*?)\\1" +
                    "|(\\*|_)(.*?)\\3" +
                    "|(~~)(.*?)\\5" +
                    "|(\\[\\[)(.*?)(?:\\|(.*?))?]]" +
                    "|([#@])(\\p{L}[\\p{L}0-9_-]*\\b)"
        )
    }

    // ✨ ЗМІНА №2: Оновлено логіку обробника кліків
    val handleClick: (Int) -> Unit = { offset ->
        // Цей рядок потрібен, щоб знайти анотації в тексті за зміщенням кліку
        val annotatedString = applyInlineStyles(text, inlineContentRegex, tagColor, projectColor, linkColor, isCompleted)

        val searchTermClicked = annotatedString.getStringAnnotations("SEARCH_TERM", start = offset, end = offset).firstOrNull()
        val obsidianLinkClicked = annotatedString.getStringAnnotations("OBSIDIAN_LINK", start = offset, end = offset).firstOrNull()

        when {
            // Якщо клікнули на тег, викликаємо onTagClick
            searchTermClicked != null -> onTagClick(searchTermClicked.item)

            // Якщо клікнули на посилання, відкриваємо Obsidian
            obsidianLinkClicked != null -> {
                val noteName = obsidianLinkClicked.item
                if (obsidianVaultName.isNotBlank()) {
                    try {
                        val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                        val encodedFile = URLEncoder.encode(noteName, "UTF-8")
                        val uri = Uri.parse("obsidian://open?vault=$encodedVault&file=$encodedFile")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "Obsidian не встановлено", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Помилка відкриття Obsidian: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Назва Obsidian Vault не вказана.", Toast.LENGTH_LONG).show()
                }
            }
            // В іншому випадку (клік по звичайному тексту) викликаємо onTextClick
            else -> onTextClick()
        }
    }

    Column(modifier = modifier) {
        text.lines().forEach { line ->
            val listMatch = listRegex.find(line)
            val (content, isList) = if (listMatch != null) {
                listMatch.destructured.component2() to true
            } else {
                line to false
            }

            val annotatedLine = applyInlineStyles(content, inlineContentRegex, tagColor, projectColor, linkColor, isCompleted)

            val fullLine = if (isList) {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("•  ") }
                    append(annotatedLine)
                }
            } else {
                annotatedLine
            }

            // ✨ ЗМІНА №3: Завжди використовуємо ClickableText для узгодженої поведінки
            ClickableText(
                text = fullLine,
                style = finalTextStyle,
                onClick = handleClick
            )
        }
    }
}

private fun applyInlineStyles(
    content: String,
    regex: Regex,
    tagColor: Color,
    projectColor: Color,
    linkColor: Color,
    isCompleted: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(content)) {
            append(content.substring(lastIndex, match.range.first))
            val (inlineContent, inlineStyle, annotation) = when {
                match.groups[2] != null -> Triple(match.groups[2]!!.value, SpanStyle(fontWeight = FontWeight.Bold), null)
                match.groups[4] != null -> Triple(match.groups[4]!!.value, SpanStyle(fontStyle = FontStyle.Italic), null)
                match.groups[6] != null -> Triple(match.groups[6]!!.value, SpanStyle(textDecoration = TextDecoration.LineThrough), null)
                match.groups[7] != null -> {
                    val linkTarget = match.groups[8]!!.value
                    val linkText = match.groups[9]?.value
                    val displayText = if (!linkText.isNullOrEmpty()) linkText else linkTarget

                    val decoration = if (isCompleted) {
                        TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    } else {
                        TextDecoration.Underline
                    }

                    Triple(displayText, SpanStyle(color = linkColor, textDecoration = decoration), "OBSIDIAN_LINK" to linkTarget)
                }
                match.groups[10] != null -> {
                    val tagSymbol = match.groups[10]!!.value
                    val tagName = match.groups[11]!!.value
                    val fullTag = "$tagSymbol$tagName"
                    val tagStyle = when (tagSymbol) {
                        "#" -> SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold)
                        "@" -> SpanStyle(color = projectColor, fontWeight = FontWeight.Medium)
                        else -> SpanStyle()
                    }
                    Triple(fullTag, tagStyle, "SEARCH_TERM" to fullTag)
                }
                else -> Triple("", SpanStyle(), null)
            }

            if (annotation != null) pushStringAnnotation(annotation.first, annotation.second)
            withStyle(style = inlineStyle) { append(inlineContent) }
            if (annotation != null) pop()
            lastIndex = match.range.last + 1
        }
        if (lastIndex < content.length) append(content.substring(lastIndex))
    }
}