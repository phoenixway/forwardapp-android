package com.romankozak.forwardappmobile.ui.theme

import androidx.compose.ui.graphics.Color

data class InputModeColors(
    val backgroundColor: Color,
    val textColor: Color,
    val inputFieldColor: Color
)

data class InputPanelColors(
    val addGoal: InputModeColors,
    val searchInList: InputModeColors,
    val searchGlobal: InputModeColors,
    val addQuickRecord: InputModeColors,
    val addProjectLog: InputModeColors
)

// Light Themes
val DefaultLightInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = Purple40.copy(alpha = 0.1f), textColor = Purple40, inputFieldColor = Purple40.copy(alpha = 0.2f)),
    searchInList = InputModeColors(backgroundColor = Pink40.copy(alpha = 0.1f), textColor = Pink40, inputFieldColor = Pink40.copy(alpha = 0.2f)),
    searchGlobal = InputModeColors(backgroundColor = Color(0xFFFFFDE7), textColor = Color(0xFFFBC02D), inputFieldColor = Color(0xFFFFF9C4)),
    addQuickRecord = InputModeColors(backgroundColor = PurpleGrey40.copy(alpha = 0.1f), textColor = PurpleGrey40, inputFieldColor = PurpleGrey40.copy(alpha = 0.2f)),
    addProjectLog = InputModeColors(backgroundColor = Color(0xFFF3E5F5), textColor = Color(0xFF7B1FA2), inputFieldColor = Color(0xFFE1BEE7))
)

val CyberpunkLightInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = CyberPink.copy(alpha = 0.1f), textColor = CyberPink, inputFieldColor = CyberPink.copy(alpha = 0.2f)),
    searchInList = InputModeColors(backgroundColor = CyberNeonMagenta.copy(alpha = 0.1f), textColor = CyberNeonMagenta, inputFieldColor = CyberNeonMagenta.copy(alpha = 0.2f)),
    searchGlobal = InputModeColors(backgroundColor = CyberLightBlue, textColor = CyberDarkGray, inputFieldColor = Color.White),
    addQuickRecord = InputModeColors(backgroundColor = CyberPink.copy(alpha = 0.2f), textColor = CyberPink, inputFieldColor = CyberPink.copy(alpha = 0.3f)),
    addProjectLog = InputModeColors(backgroundColor = CyberNeonMagenta.copy(alpha = 0.2f), textColor = CyberNeonMagenta, inputFieldColor = CyberNeonMagenta.copy(alpha = 0.3f))
)

val SciFiLightInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = SciFiLightBlue, textColor = SciFiDarkBlue, inputFieldColor = Color.White),
    searchInList = InputModeColors(backgroundColor = SciFiMidGray.copy(alpha = 0.2f), textColor = SciFiDarkBlue, inputFieldColor = SciFiMidGray.copy(alpha = 0.3f)),
    searchGlobal = InputModeColors(backgroundColor = SciFiLightBlue, textColor = SciFiDarkBlue, inputFieldColor = Color.White),
    addQuickRecord = InputModeColors(backgroundColor = SciFiMidGray.copy(alpha = 0.3f), textColor = SciFiDarkBlue, inputFieldColor = SciFiMidGray.copy(alpha = 0.4f)),
    addProjectLog = InputModeColors(backgroundColor = SciFiLightBlue, textColor = SciFiDarkBlue, inputFieldColor = Color.White)
)

// Dark Themes
val DefaultDarkInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = Purple80.copy(alpha = 0.1f), textColor = Purple80, inputFieldColor = Purple80.copy(alpha = 0.2f)),
    searchInList = InputModeColors(backgroundColor = Pink80.copy(alpha = 0.1f), textColor = Pink80, inputFieldColor = Pink80.copy(alpha = 0.2f)),
    searchGlobal = InputModeColors(backgroundColor = Color(0xFFFF8F00).copy(alpha = 0.2f), textColor = Color(0xFFFFECB3), inputFieldColor = Color(0xFFFF8F00).copy(alpha = 0.3f)),
    addQuickRecord = InputModeColors(backgroundColor = PurpleGrey80.copy(alpha = 0.1f), textColor = PurpleGrey80, inputFieldColor = PurpleGrey80.copy(alpha = 0.2f)),
    addProjectLog = InputModeColors(backgroundColor = Pink80.copy(alpha = 0.2f), textColor = Pink80, inputFieldColor = Pink80.copy(alpha = 0.3f))
)

val CyberpunkDarkInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = CyberNeonCyan.copy(alpha = 0.1f), textColor = CyberNeonCyan, inputFieldColor = CyberNeonCyan.copy(alpha = 0.2f)),
    searchInList = InputModeColors(backgroundColor = CyberNeonMagenta.copy(alpha = 0.1f), textColor = CyberNeonMagenta, inputFieldColor = CyberNeonMagenta.copy(alpha = 0.2f)),
    searchGlobal = InputModeColors(backgroundColor = CyberNeonCyan.copy(alpha = 0.2f), textColor = CyberNeonCyan, inputFieldColor = CyberNeonCyan.copy(alpha = 0.3f)),
    addQuickRecord = InputModeColors(backgroundColor = CyberNeonMagenta.copy(alpha = 0.2f), textColor = CyberNeonMagenta, inputFieldColor = CyberNeonMagenta.copy(alpha = 0.3f)),
    addProjectLog = InputModeColors(backgroundColor = CyberDarkBlue, textColor = CyberLightGray, inputFieldColor = CyberDarkBlue.copy(alpha = 0.8f))
)

val SciFiDarkInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = SciFiCyan.copy(alpha = 0.1f), textColor = SciFiCyan, inputFieldColor = SciFiCyan.copy(alpha = 0.2f)),
    searchInList = InputModeColors(backgroundColor = SciFiSilver.copy(alpha = 0.1f), textColor = SciFiSilver, inputFieldColor = SciFiSilver.copy(alpha = 0.2f)),
    searchGlobal = InputModeColors(backgroundColor = SciFiCyan.copy(alpha = 0.2f), textColor = SciFiCyan, inputFieldColor = SciFiCyan.copy(alpha = 0.3f)),
    addQuickRecord = InputModeColors(backgroundColor = SciFiSilver.copy(alpha = 0.2f), textColor = SciFiSilver, inputFieldColor = SciFiSilver.copy(alpha = 0.3f)),
    addProjectLog = InputModeColors(backgroundColor = SciFiDeepBlue, textColor = SciFiSilver, inputFieldColor = SciFiDeepBlue.copy(alpha = 0.8f))
)

val DraculaInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = DraculaCurrentLine, textColor = DraculaPink, inputFieldColor = DraculaBackground),
    searchInList = InputModeColors(backgroundColor = DraculaCurrentLine, textColor = DraculaPurple, inputFieldColor = DraculaBackground),
    searchGlobal = InputModeColors(backgroundColor = DraculaCurrentLine, textColor = DraculaCyan, inputFieldColor = DraculaBackground),
    addQuickRecord = InputModeColors(backgroundColor = DraculaCurrentLine, textColor = DraculaGreen, inputFieldColor = DraculaBackground),
    addProjectLog = InputModeColors(backgroundColor = DraculaCurrentLine, textColor = DraculaOrange, inputFieldColor = DraculaBackground)
)

val NordInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = Nord1, textColor = Nord8, inputFieldColor = Nord0),
    searchInList = InputModeColors(backgroundColor = Nord1, textColor = Nord11, inputFieldColor = Nord0),
    searchGlobal = InputModeColors(backgroundColor = Nord1, textColor = Nord14, inputFieldColor = Nord0),
    addQuickRecord = InputModeColors(backgroundColor = Nord3, textColor = Nord8, inputFieldColor = Nord1),
    addProjectLog = InputModeColors(backgroundColor = Nord3, textColor = Nord11, inputFieldColor = Nord1)
)

val SolarizedDarkInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = SolarizedBase02, textColor = SolarizedBlue, inputFieldColor = SolarizedBase03),
    searchInList = InputModeColors(backgroundColor = SolarizedBase02, textColor = SolarizedMagenta, inputFieldColor = SolarizedBase03),
    searchGlobal = InputModeColors(backgroundColor = SolarizedBase02, textColor = SolarizedYellow, inputFieldColor = SolarizedBase03),
    addQuickRecord = InputModeColors(backgroundColor = SolarizedBase02, textColor = SolarizedGreen, inputFieldColor = SolarizedBase03),
    addProjectLog = InputModeColors(backgroundColor = SolarizedBase02, textColor = SolarizedOrange, inputFieldColor = SolarizedBase03)
)

val TerminalGreenInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = TerminalSurface, textColor = TerminalNeon, inputFieldColor = TerminalGrid),
    searchInList = InputModeColors(backgroundColor = TerminalSurface, textColor = TerminalNeonDim, inputFieldColor = TerminalGrid),
    searchGlobal = InputModeColors(backgroundColor = TerminalSurface, textColor = TerminalAmber, inputFieldColor = TerminalGrid),
    addQuickRecord = InputModeColors(backgroundColor = TerminalGrid, textColor = TerminalNeon, inputFieldColor = TerminalSurface),
    addProjectLog = InputModeColors(backgroundColor = TerminalSurface, textColor = TerminalAmber, inputFieldColor = TerminalGrid)
)

val EmeraldInputPanelColors = InputPanelColors(
    addGoal = InputModeColors(backgroundColor = EmeraldSurface, textColor = EmeraldMint, inputFieldColor = EmeraldDeep),
    searchInList = InputModeColors(backgroundColor = EmeraldSurface, textColor = EmeraldLeaf, inputFieldColor = EmeraldDeep),
    searchGlobal = InputModeColors(backgroundColor = EmeraldSurface, textColor = EmeraldLime, inputFieldColor = EmeraldDeep),
    addQuickRecord = InputModeColors(backgroundColor = EmeraldDeep, textColor = EmeraldMint, inputFieldColor = EmeraldSurface),
    addProjectLog = InputModeColors(backgroundColor = EmeraldSurface, textColor = EmeraldGray, inputFieldColor = EmeraldDeep)
)
