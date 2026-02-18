package com.romankozak.forwardappmobile.features.attachments.specific_types.musicnote

object MusicNoteToMusicXmlConverter {
    private val tokenRegex = Regex("^([a-gA-G])([#b]?)(\\d)$")

    fun convert(content: String, title: String): String {
        val measures = mutableListOf<MutableList<ParsedNote>>()
        measures += mutableListOf()
        var beatsInMeasure = 0

        content.lineSequence().forEach { line ->
            line.split(Regex("\\s+")).forEach { tokenRaw ->
                val token = tokenRaw.trim().trim('.', ',', ';', ':')
                if (token.isEmpty()) return@forEach
                if (token == "|") {
                    if (measures.last().isNotEmpty()) {
                        measures += mutableListOf()
                        beatsInMeasure = 0
                    }
                    return@forEach
                }
                val match = tokenRegex.matchEntire(token) ?: return@forEach
                val step = match.groupValues[1].uppercase()
                val accidental = match.groupValues[2]
                val octave = match.groupValues[3].toIntOrNull() ?: return@forEach
                val alter =
                    when (accidental) {
                        "#" -> 1
                        "b" -> -1
                        else -> 0
                    }

                if (beatsInMeasure >= 4) {
                    measures += mutableListOf()
                    beatsInMeasure = 0
                }
                measures.last().add(ParsedNote(step = step, octave = octave, alter = alter))
                beatsInMeasure += 1
            }
        }

        val normalizedMeasures = measures.filter { it.isNotEmpty() }.toMutableList()
        if (normalizedMeasures.isEmpty()) {
            normalizedMeasures += mutableListOf(ParsedNote(step = "C", octave = 4, alter = 0, isRest = true, duration = 4, type = "whole"))
        }

        val part = buildString {
            normalizedMeasures.forEachIndexed { index, notes ->
                append("<measure number=\"")
                append(index + 1)
                append("\">")
                if (index == 0) {
                    append("<attributes>")
                    append("<divisions>1</divisions>")
                    append("<key><fifths>0</fifths></key>")
                    append("<time><beats>4</beats><beat-type>4</beat-type></time>")
                    append("<clef><sign>G</sign><line>2</line></clef>")
                    append("</attributes>")
                }
                notes.forEach { note ->
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
                    append(note.duration)
                    append("</duration>")
                    append("<type>")
                    append(note.type)
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

    private data class ParsedNote(
        val step: String,
        val octave: Int,
        val alter: Int,
        val isRest: Boolean = false,
        val duration: Int = 1,
        val type: String = "quarter",
    )
}
