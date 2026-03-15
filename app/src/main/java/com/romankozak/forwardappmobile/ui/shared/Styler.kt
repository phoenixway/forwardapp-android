package com.romankozak.forwardappmobile.ui.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun AnnotatedString.Builder.styleLine(
    line: String,
    textColor: Color,
    accentColor: Color,
) {
    val boldRegex = Regex("""\*\*(.*?)\*\*""")

    fun applyBold(
        text: String,
        baseStyle: SpanStyle,
    ) {
        var lastIndex = 0
        boldRegex.findAll(text).forEach { match ->
            val range = match.range
            val boldText = match.groupValues[1]
            if (range.first > lastIndex) {
                withStyle(baseStyle) { append(text.substring(lastIndex, range.first)) }
            }
            withStyle(baseStyle.copy(fontWeight = FontWeight.Bold)) {
                append(boldText)
            }
            lastIndex = range.last + 1
        }
        if (lastIndex < text.length) {
            withStyle(baseStyle) { append(text.substring(lastIndex)) }
        }
    }

    val matched =
        lineStyleRules(accentColor, textColor).any { rule ->
            rule.regex.find(line)?.let { match ->
                rule.renderer(this, match.destructured.toList(), ::applyBold)
            } != null
        }

    if (!matched) {
        applyBold(line, SpanStyle(color = textColor))
    }
}

private data class LineStyleRule(
    val regex: Regex,
    val renderer: (AnnotatedString.Builder, List<String>, (String, SpanStyle) -> Unit) -> Unit,
)

private fun lineStyleRules(accentColor: Color, textColor: Color): List<LineStyleRule> =
    listOf(
        lineStyleRule(Regex("""^(\s*)(#+\s)(.*)""")) { indent, marker, content, applyBoldText ->
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append(marker) }
            applyBoldText(content, SpanStyle(color = textColor, fontWeight = FontWeight.Bold))
        },
        lineStyleRule(Regex("""^(\s*)\*\s(.*)""")) { indent, _, content, applyBoldText ->
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("• ") }
            applyBoldText(content, SpanStyle(color = textColor))
        },
        lineStyleRule(Regex("""^(\s*)(\d+)\.\s(.*)""")) { indent, marker, content, applyBoldText ->
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("$marker. ") }
            applyBoldText(content, SpanStyle(color = textColor))
        },
        lineStyleRule(Regex("""^(\s*)\[\s\]\s(.*)""")) { indent, _, content, applyBoldText ->
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☐ ") }
            applyBoldText(content, SpanStyle(color = textColor))
        },
        lineStyleRule(Regex("""^(\s*)\[x\]\s(.*)""", RegexOption.IGNORE_CASE)) { indent, _, content, applyBoldText ->
            append(indent)
            withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☑ ") }
            applyBoldText(content, SpanStyle(color = textColor))
        },
    )

private fun lineStyleRule(
    regex: Regex,
    renderer: AnnotatedString.Builder.(String, String, String, (String, SpanStyle) -> Unit) -> Unit,
): LineStyleRule =
    LineStyleRule(regex = regex) { builder, parts, applyBoldText ->
        val indent = parts.getOrElse(0) { "" }
        val marker = parts.getOrElse(1) { "" }
        val content = parts.getOrElse(2) { "" }
        builder.renderer(indent, marker, content, applyBoldText)
    }
