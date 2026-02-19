package com.romankozak.forwardappmobile.features.attachments.specific_types.musicnote

object MusicNoteToMusicXmlConverter {
    private const val DIVISIONS_PER_QUARTER = 8
    private val noteRegex = Regex("^([a-gA-G])([#b]{0,2})(\\d+)?([+-]*)$")
    private val octaveDirectiveRegex = Regex("^@?oct\\s*=\\s*(-?\\d+)\\s*$", RegexOption.IGNORE_CASE)
    private val durationDirectiveRegex = Regex("^@?dur\\s*=\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)
    private val meterDirectiveRegex = Regex("^@?m\\s*=\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)

    fun convert(content: String, title: String): String {
        val measures = mutableListOf(ParsedMeasure())
        var durationInMeasure = 0
        var defaultOctave = 4
        var defaultDurationDenominator = 4
        var beatsPerMeasure = 4
        var beatUnit = 4
        var isDurationModeEnabled = false

        content.lineSequence().forEach { line ->
            line.split(Regex("\\s+")).forEach { tokenRaw ->
                val token = tokenRaw.trim().trim('.', ',', ';', ':')
                if (token.isEmpty()) return@forEach

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

                octaveDirectiveRegex.matchEntire(token)?.let { match ->
                    defaultOctave = match.groupValues[1].toIntOrNull() ?: defaultOctave
                    return@forEach
                }

                durationDirectiveRegex.matchEntire(token)?.let { match ->
                    defaultDurationDenominator =
                        normalizeDurationDenominator(match.groupValues[1].toIntOrNull())
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

                if (token == "|") {
                    if (currentMeasure().notes.isNotEmpty()) {
                        startNewMeasure()
                    }
                    return@forEach
                }

                val match = noteRegex.matchEntire(token) ?: return@forEach
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
                currentMeasure().notes.add(
                    ParsedNote(
                        step = step,
                        octave = octave,
                        alter = alter,
                        durationDenominator = durationDenominator,
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
                    notes = mutableListOf(ParsedNote(step = "C", octave = defaultOctave, alter = 0, isRest = true, durationDenominator = 1)),
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
                measure.notes.forEach { note ->
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
                    append("</note>")
                }
                append("</measure>")
            }
        }

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE score-partwise PUBLIC
              "-//Recordare//DTD MusicXML 3.1 Partwise//EN"
              "http://www.musicxml.org/dtds/partwise.dtd">
            <score-partwise version="3.1">
              <work><work-title>${xmlEscape(title.ifBlank { "Music note" })}</work-title></work>
              <part-list>
                <score-part id="P1">
                  <part-name>Music</part-name>
                </score-part>
              </part-list>
              <part id="P1">$part</part>
            </score-partwise>
        """.trimIndent()
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

    private data class ParsedNote(
        val step: String,
        val octave: Int,
        val alter: Int,
        val isRest: Boolean = false,
        val durationDenominator: Int = 4,
    )

    private data class ParsedMeasure(
        var beats: Int = 4,
        var beatType: Int = 4,
        val notes: MutableList<ParsedNote> = mutableListOf(),
    )
}
