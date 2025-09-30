package com.romankozak.forwardappmobile.ui.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun AnnotatedString.Builder.styleLine(line: String, textColor: Color, accentColor: Color) {
    val headingRegex   = Regex("""^(\s*)(#+\s)(.*)""")
    val bulletRegex    = Regex("""^(\s*)\*\s(.*)""")
    val numberedRegex  = Regex("""^(\s*)(\d+)\.\s(.*)""")
    val checkedRegex   = Regex("""^(\s*)\[x\]\s(.*)""", RegexOption.IGNORE_CASE)
    val uncheckedRegex = Regex("""^(\s*)\[\s\]\s(.*)""")
    val boldRegex = Regex("""\*\*(.*?)\*\*""")

    fun applyBold(text: String, baseStyle: SpanStyle) {
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

    var matched = false

    if (!matched) headingRegex.find(line)?.let {
        val (indent, hashes, content) = it.destructured
        append(indent)
        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append(hashes) }
        applyBold(content, SpanStyle(color = textColor, fontWeight = FontWeight.Bold))
        matched = true
    }

    if (!matched) bulletRegex.find(line)?.let {
        val (indent, content) = it.destructured
        append(indent)
        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("• ") }
        applyBold(content, SpanStyle(color = textColor))
        matched = true
    }
    if (!matched) numberedRegex.find(line)?.let {
        val (indent, number, content) = it.destructured
        append(indent)
        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("$number. ") }
        applyBold(content, SpanStyle(color = textColor))
        matched = true
    }
    if (!matched) uncheckedRegex.find(line)?.let {
        val (indent, content) = it.destructured
        append(indent)
        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☐ ") }
        applyBold(content, SpanStyle(color = textColor))
        matched = true
    }
    if (!matched) checkedRegex.find(line)?.let {
        val (indent, content) = it.destructured
        append(indent)
        withStyle(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) { append("☑ ") }
        applyBold(content, SpanStyle(color = textColor))
        matched = true
    }

    if (!matched) {
        applyBold(line, SpanStyle(color = textColor))
    }
}