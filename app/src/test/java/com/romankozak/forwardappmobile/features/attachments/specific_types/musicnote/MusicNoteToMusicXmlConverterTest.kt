package com.romankozak.forwardappmobile.features.attachments.specific_types.musicnote

import org.junit.Assert.assertTrue
import org.junit.Test

class MusicNoteToMusicXmlConverterTest {
    @Test
    fun numericSuffixIsDurationAndOctaveComesFromDirective() {
        val xml =
            MusicNoteToMusicXmlConverter.convert(
                content = "@oct=5 C4 C8 C16",
                title = "t",
            )

        assertTrue(xml.contains("<octave>5</octave>"))
        assertTrue(xml.contains("<type>quarter</type>"))
        assertTrue(xml.contains("<type>eighth</type>"))
        assertTrue(xml.contains("<type>16th</type>"))
    }

    @Test
    fun doubleBarlineTokenProducesMusicXmlDoubleBarline() {
        val xml =
            MusicNoteToMusicXmlConverter.convert(
                content = "@oct=4 @dur=4 C || D",
                title = "t",
            )

        assertTrue(xml.contains("<bar-style>light-light</bar-style>"))
    }

    @Test
    fun octaveShiftsUsePlusMinusOnly() {
        val xml =
            MusicNoteToMusicXmlConverter.convert(
                content = "@oct=3 @dur=4 C C+ C-",
                title = "t",
            )

        assertTrue(xml.contains("<octave>3</octave>"))
        assertTrue(xml.contains("<octave>4</octave>"))
        assertTrue(xml.contains("<octave>2</octave>"))
    }

    @Test
    fun parenthesisPhraseCreatesSlurStartAndStop() {
        val xml =
            MusicNoteToMusicXmlConverter.convert(
                content = "@oct=4 @dur=4 ( C D )",
                title = "t",
            )

        assertTrue(xml.contains("<slur type=\"start\"/>"))
        assertTrue(xml.contains("<slur type=\"stop\"/>"))
    }

    @Test
    fun commaCreatesBreathMark() {
        val xml =
            MusicNoteToMusicXmlConverter.convert(
                content = "@oct=4 @dur=4 C, D",
                title = "t",
            )

        assertTrue(xml.contains("<breath-mark/>"))
    }

    @Test
    fun emptyLineCreatesSystemBreak() {
        val xml =
            MusicNoteToMusicXmlConverter.convert(
                content =
                    """
                    @oct=4 @dur=4
                    C D
                    
                    E F
                    """.trimIndent(),
                title = "t",
            )

        assertTrue(xml.contains("<print new-system=\"yes\"/>"))
    }
}
