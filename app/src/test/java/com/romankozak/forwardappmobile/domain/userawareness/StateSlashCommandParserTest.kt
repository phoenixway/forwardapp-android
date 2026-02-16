package com.romankozak.forwardappmobile.domain.userawareness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StateSlashCommandParserTest {
    private val parser = StateSlashCommandParser()

    @Test
    fun noCommand_keepsText() {
        val result = parser.parse("worked 2h on docs")
        assertEquals("worked 2h on docs", result.cleanedText)
        assertNull(result.detectedChange)
    }

    @Test
    fun commandInBeginningMiddleEnd_isRemoved() {
        val atBeginning = parser.parse("/normal worked")
        val inMiddle = parser.parse("worked /exhaustion today")
        val atEnd = parser.parse("worked today /unproductive")

        assertEquals("worked", atBeginning.cleanedText)
        assertEquals("worked today", inMiddle.cleanedText)
        assertEquals("worked today", atEnd.cleanedText)
    }

    @Test
    fun multipleCommands_lastWins() {
        val result = parser.parse("notes /unproductive /crisis 3")
        assertEquals("notes", result.cleanedText)
        assertEquals(UserAwarenessStateType.CRISIS, result.detectedChange?.type)
        assertEquals(3, result.detectedChange?.crisisLevel)
    }

    @Test
    fun crisisWithoutLevel_defaultsToOne() {
        val result = parser.parse("/crisis\nworked 2h")
        assertEquals("worked 2h", result.cleanedText)
        assertEquals(UserAwarenessStateType.CRISIS, result.detectedChange?.type)
        assertEquals(1, result.detectedChange?.crisisLevel)
        assertNull(result.detectedChange?.label)
    }

    @Test
    fun crisisInvalidLevel_isIgnored() {
        val result = parser.parse("done /crisis 7 too much")
        assertEquals("done /crisis 7 too much", result.cleanedText)
        assertNull(result.detectedChange)
    }

    @Test
    fun labelTrimmedAndMaxLen80() {
        val longLabel = "x".repeat(120)
        val result = parser.parse("/crisis 2   $longLabel   ")
        assertEquals(80, result.detectedChange?.label?.length)
        assertEquals(2, result.detectedChange?.crisisLevel)
    }

    @Test
    fun urlLikeToken_isNotParsed() {
        val result = parser.parse("https://x/y /normal")
        assertEquals("https://x/y", result.cleanedText)
        assertEquals(UserAwarenessStateType.NORMAL, result.detectedChange?.type)
    }
}
