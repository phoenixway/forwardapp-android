package com.romankozak.forwardappmobile.features.attachments.specific_types.musicnote

import java.util.ArrayDeque

object MusicNoteToMusicXmlConverter {
    private const val DIVISIONS_PER_QUARTER = 8

    private val noteRegex = Regex("^([a-gA-G])([#b]{0,2})(\\d+)?([+-]*)$")
    private val restRegex = Regex("^[rR](\\d+)?$")
    private val octaveDirectiveRegex = Regex("^@?oct\\s*=\\s*(-?\\d+)\\s*$", RegexOption.IGNORE_CASE)
    private val durationDirectiveRegex = Regex("^@?dur\\s*=\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)
    private val meterDirectiveRegex = Regex("^@?m\\s*=\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)
    private val titleDirectiveRegex = Regex("^@?title\\s*=\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val composerDirectiveRegex = Regex("^@?composer\\s*=\\s*(.+)$", RegexOption.IGNORE_CASE)

    fun convert(content: String, title: String): String {
        val measures = mutableListOf(ParsedMeasure())

        var durationInMeasure = 0
        var defaultOctave = 4
        var defaultDurationDenominator = 4
        var beatsPerMeasure = 4
        var beatUnit = 4
        var isDurationModeEnabled = false

        var resolvedTitle = title.ifBlank { "Music note" }
        var composer: String? = null

        val pendingAboveTexts = ArrayDeque<String>()
        val pendingLyrics = ArrayDeque<String>()

        fun currentMeasure(): ParsedMeasure = measures.last()

        fun startNewMeasure() {
            measures.add(
                ParsedMeasure(
                    beats = beatsPerMeasure,
                    beatType = beatUnit,
                ),
            )
            durationInMeasure = 0
        }

        fun attachMeasureComment(text: String) {
            val cleaned = text.trim()
            if (cleaned.isEmpty()) return
            val targetIndex =
                if (currentMeasure().notes.isEmpty() && measures.size > 1) {
                    measures.lastIndex - 1
                } else {
                    measures.lastIndex
                }
            if (targetIndex >= 0) {
                measures[targetIndex].measureDirections.add(cleaned)
            }
        }

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("#") || line.startsWith("//")) return@forEach

            if (line.startsWith("^")) {
                val text = line.removePrefix("^").trim()
                if (text.isNotEmpty()) {
                    pendingAboveTexts.addLast(text)
                }
                return@forEach
            }

            if (line.startsWith("_")) {
                val lyricLine = line.removePrefix("_").trim()
                if (lyricLine.isNotEmpty()) {
                    lyricLine.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { pendingLyrics.addLast(it) }
                }
                return@forEach
            }

            tokenizeLine(line).forEach { tokenRaw ->
                val token = tokenRaw.trim()
                if (token.isEmpty()) return@forEach

                octaveDirectiveRegex.matchEntire(token)?.let { match ->
                    defaultOctave = match.groupValues[1].toIntOrNull() ?: defaultOctave
                    return@forEach
                }

                durationDirectiveRegex.matchEntire(token)?.let { match ->
                    defaultDurationDenominator = normalizeDurationDenominator(match.groupValues[1].toIntOrNull())
                    isDurationModeEnabled = true
                    return@forEach
                }

                meterDirectiveRegex.matchEntire(token)?.let { match ->
                    val parsedBeats = match.groupValues[1].toIntOrNull()
                    val parsedBeatUnit = normalizeBeatType(match.groupValues[2].toIntOrNull())
                    if (parsedBeats != null && parsedBeats > 0) {
                        beatsPerMeasure = parsedBeats
                    }
                    beatUnit = parsedBeatUnit

                    if (currentMeasure().notes.isEmpty()) {
                        currentMeasure().beats = beatsPerMeasure
                        currentMeasure().beatType = beatUnit
                    } else {
                        startNewMeasure()
                    }
                    return@forEach
                }

                titleDirectiveRegex.matchEntire(token)?.let { match ->
                    val value = unquote(match.groupValues[1])
                    if (value.isNotBlank()) {
                        resolvedTitle = value
                    }
                    return@forEach
                }

                composerDirectiveRegex.matchEntire(token)?.let { match ->
                    val value = unquote(match.groupValues[1])
                    composer = value.takeIf { it.isNotBlank() }
                    return@forEach
                }

                if (token == "|") {
                    if (currentMeasure().notes.isNotEmpty()) {
                        startNewMeasure()
                    }
                    return@forEach
                }

                if (token.startsWith("^") && token.length > 1) {
                    pendingAboveTexts.addLast(token.removePrefix("^").trim())
                    return@forEach
                }

                if (token.startsWith("_") && token.length > 1) {
                    token
                        .removePrefix("_")
                        .split(Regex("\\s+"))
                        .filter { it.isNotBlank() }
                        .forEach { pendingLyrics.addLast(it) }
                    return@forEach
                }

                if (token.startsWith("{") && token.endsWith("}")) {
                    attachMeasureComment(token.removeSurrounding("{", "}"))
                    return@forEach
                }

                val cleanedToken = token.trim('.', ',', ';', ':')
                if (cleanedToken.isEmpty()) return@forEach

                val inlineComment = extractInlineComment(cleanedToken)
                val noteToken = inlineComment.base
                val commentText = inlineComment.comment

                restRegex.matchEntire(noteToken)?.let { restMatch ->
                    val numericSuffix = restMatch.groupValues[1].toIntOrNull()
                    val durationDenominator =
                        if (isDurationModeEnabled) {
                            normalizeDurationDenominator(numericSuffix ?: defaultDurationDenominator)
                        } else {
                            defaultDurationDenominator
                        }
                    val xmlDuration = durationValueFromDenominator(durationDenominator)
                    val measureDuration = measureDurationFromTime(currentMeasure().beats, currentMeasure().beatType)

                    if (durationInMeasure + xmlDuration > measureDuration && currentMeasure().notes.isNotEmpty()) {
                        startNewMeasure()
                    }

                    currentMeasure().notes.add(
                        ParsedNote(
                            step = "C",
                            octave = defaultOctave,
                            alter = 0,
                            isRest = true,
                            durationDenominator = durationDenominator,
                        ),
                    )
                    durationInMeasure += xmlDuration
                    return@forEach
                }

                val match = noteRegex.matchEntire(noteToken) ?: return@forEach
                val step = match.groupValues[1].uppercase()
                val accidental = match.groupValues[2]
                val numericSuffix = match.groupValues[3].toIntOrNull()
                val octaveShift = match.groupValues[4]

                val alter = accidental.count { it == '#' } - accidental.count { it == 'b' }
                val durationDenominator =
                    if (isDurationModeEnabled) {
                        normalizeDurationDenominator(numericSuffix ?: defaultDurationDenominator)
                    } else {
                        defaultDurationDenominator
                    }
                val xmlDuration = durationValueFromDenominator(durationDenominator)
                val octaveBase = if (isDurationModeEnabled) defaultOctave else (numericSuffix ?: defaultOctave)
                val octave = octaveBase + octaveShift.sumOf { if (it == '+') 1 else -1 }

                val measureDuration = measureDurationFromTime(currentMeasure().beats, currentMeasure().beatType)
                if (durationInMeasure + xmlDuration > measureDuration && currentMeasure().notes.isNotEmpty()) {
                    startNewMeasure()
                }

                val noteDirections = mutableListOf<String>()
                while (pendingAboveTexts.isNotEmpty()) {
                    noteDirections.add(pendingAboveTexts.removeFirst())
                }
                if (!commentText.isNullOrBlank()) {
                    noteDirections.add(commentText)
                }

                val lyricText = if (pendingLyrics.isNotEmpty()) pendingLyrics.removeFirst() else null

                currentMeasure().notes.add(
                    ParsedNote(
                        step = step,
                        octave = octave,
                        alter = alter,
                        durationDenominator = durationDenominator,
                        lyricText = lyricText,
                        noteDirections = noteDirections,
                    ),
                )
                durationInMeasure += xmlDuration
            }
        }

        val normalizedMeasures = measures.filter { it.notes.isNotEmpty() }.toMutableList()
        if (normalizedMeasures.isEmpty()) {
            normalizedMeasures +=
                ParsedMeasure(
                    beats = beatsPerMeasure,
                    beatType = beatUnit,
                    notes =
                        mutableListOf(
                            ParsedNote(
                                step = "C",
                                octave = defaultOctave,
                                alter = 0,
                                isRest = true,
                                durationDenominator = 1,
                            ),
                        ),
                )
        }

        val part = buildString {
            normalizedMeasures.forEachIndexed { index, measure ->
                append("<measure number=\"")
                append(index + 1)
                append("\">")

                if (index == 0) {
                    append("<attributes>")
                    append("<divisions>")
                    append(DIVISIONS_PER_QUARTER)
                    append("</divisions>")
                    append("<key><fifths>0</fifths></key>")
                    append("<time><beats>")
                    append(measure.beats)
                    append("</beats><beat-type>")
                    append(measure.beatType)
                    append("</beat-type></time>")
                    append("<clef><sign>G</sign><line>2</line></clef>")
                    append("</attributes>")
                } else {
                    val previous = normalizedMeasures[index - 1]
                    if (previous.beats != measure.beats || previous.beatType != measure.beatType) {
                        append("<attributes>")
                        append("<time><beats>")
                        append(measure.beats)
                        append("</beats><beat-type>")
                        append(measure.beatType)
                        append("</beat-type></time>")
                        append("</attributes>")
                    }
                }

                measure.measureDirections.forEach { directionText ->
                    appendDirectionText(directionText, this)
                }

                measure.notes.forEach { note ->
                    note.noteDirections.forEach { directionText ->
                        appendDirectionText(directionText, this)
                    }

                    val noteDuration = durationValueFromDenominator(note.durationDenominator)
                    val noteType = typeFromDenominator(note.durationDenominator)

                    append("<note>")
                    if (note.isRest) {
                        append("<rest/>")
                    } else {
                        append("<pitch><step>")
                        append(note.step)
                        append("</step>")
                        if (note.alter != 0) {
                            append("<alter>")
                            append(note.alter)
                            append("</alter>")
                        }
                        append("<octave>")
                        append(note.octave)
                        append("</octave></pitch>")
                    }
                    append("<duration>")
                    append(noteDuration)
                    append("</duration>")
                    append("<type>")
                    append(noteType)
                    append("</type>")
                    if (!note.isRest && !note.lyricText.isNullOrBlank()) {
                        append("<lyric><text>")
                        append(xmlEscape(note.lyricText))
                        append("</text></lyric>")
                    }
                    append("</note>")
                }
                append("</measure>")
            }
        }

        val composerXml =
            composer
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    "<identification><creator type=\"composer\">${xmlEscape(it)}</creator></identification>"
                }
                .orEmpty()

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE score-partwise PUBLIC
              "-//Recordare//DTD MusicXML 3.1 Partwise//EN"
              "http://www.musicxml.org/dtds/partwise.dtd">
            <score-partwise version="3.1">
              <work><work-title>${xmlEscape(resolvedTitle)}</work-title></work>
              $composerXml
              <part-list>
                <score-part id="P1">
                  <part-name>Music</part-name>
                </score-part>
              </part-list>
              <part id="P1">$part</part>
            </score-partwise>
        """.trimIndent()
    }

    private fun appendDirectionText(
        text: String,
        builder: StringBuilder,
    ) {
        builder.append("<direction placement=\"above\">")
        builder.append("<direction-type><words>")
        builder.append(xmlEscape(text))
        builder.append("</words></direction-type>")
        builder.append("</direction>")
    }

    private fun extractInlineComment(token: String): InlineComment {
        val openIndex = token.indexOf('{')
        val closeIndex = token.lastIndexOf('}')
        if (openIndex <= 0 || closeIndex <= openIndex || closeIndex != token.lastIndex) {
            return InlineComment(base = token, comment = null)
        }
        val base = token.substring(0, openIndex)
        val comment = token.substring(openIndex + 1, closeIndex).trim()
        return InlineComment(base = base, comment = comment.takeIf { it.isNotEmpty() })
    }

    private fun unquote(value: String): String {
        val trimmed = value.trim()
        if (trimmed.length >= 2 && ((trimmed.startsWith('"') && trimmed.endsWith('"')) ||
                (trimmed.startsWith('\'') && trimmed.endsWith('\'')))) {
            return trimmed.substring(1, trimmed.length - 1).trim()
        }
        return trimmed
    }

    private fun tokenizeLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0

        while (i < line.length) {
            while (i < line.length && line[i].isWhitespace()) i++
            if (i >= line.length) break

            if (line[i] == '#') break
            if (i + 1 < line.length && line[i] == '/' && line[i + 1] == '/') break

            if (line[i] == '{') {
                val end = line.indexOf('}', startIndex = i + 1)
                if (end == -1) {
                    tokens.add(line.substring(i))
                    break
                }
                tokens.add(line.substring(i, end + 1))
                i = end + 1
                continue
            }

            val sb = StringBuilder()
            while (i < line.length && !line[i].isWhitespace()) {
                val ch = line[i]
                if (ch == '"') {
                    sb.append(ch)
                    i++
                    while (i < line.length) {
                        val q = line[i]
                        sb.append(q)
                        i++
                        if (q == '"') break
                    }
                    continue
                }
                sb.append(ch)
                i++
            }
            if (sb.isNotEmpty()) {
                tokens.add(sb.toString())
            }
        }

        return tokens
    }

    private fun xmlEscape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun normalizeDurationDenominator(raw: Int?): Int {
        val value = raw ?: return 4
        if (value <= 0) return 4
        return when (value) {
            1, 2, 4, 8, 16, 32 -> value
            else -> 4
        }
    }

    private fun durationValueFromDenominator(denominator: Int): Int =
        (DIVISIONS_PER_QUARTER * 4) / normalizeDurationDenominator(denominator)

    private fun normalizeBeatType(raw: Int?): Int {
        val value = raw ?: return 4
        return when (value) {
            1, 2, 4, 8, 16, 32 -> value
            else -> 4
        }
    }

    private fun measureDurationFromTime(
        beats: Int,
        beatType: Int,
    ): Int = beats * ((DIVISIONS_PER_QUARTER * 4) / normalizeBeatType(beatType))

    private fun typeFromDenominator(denominator: Int): String =
        when (normalizeDurationDenominator(denominator)) {
            1 -> "whole"
            2 -> "half"
            4 -> "quarter"
            8 -> "eighth"
            16 -> "16th"
            32 -> "32nd"
            else -> "quarter"
        }

    private data class InlineComment(
        val base: String,
        val comment: String?,
    )

    private data class ParsedNote(
        val step: String,
        val octave: Int,
        val alter: Int,
        val isRest: Boolean = false,
        val durationDenominator: Int = 4,
        val lyricText: String? = null,
        val noteDirections: List<String> = emptyList(),
    )

    private data class ParsedMeasure(
        var beats: Int = 4,
        var beatType: Int = 4,
        val notes: MutableList<ParsedNote> = mutableListOf(),
        val measureDirections: MutableList<String> = mutableListOf(),
    )
}
