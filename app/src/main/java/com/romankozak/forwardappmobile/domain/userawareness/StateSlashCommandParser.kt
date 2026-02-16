package com.romankozak.forwardappmobile.domain.userawareness

data class ParsedStateCommand(
    val rawCommand: String,
    val change: UserStateChange,
    val rangeStart: Int,
    val rangeEndExclusive: Int,
)

data class StateCommandParseResult(
    val cleanedText: String,
    val detectedChange: UserStateChange?,
    val allCommandsFound: List<ParsedStateCommand>,
)

class StateSlashCommandParser
    @javax.inject.Inject
    constructor() {
    private val nonCrisisPattern =
        Regex("""(?<!:)(?<!\S)/(normal|exhaustion|unproductive)\b""", RegexOption.IGNORE_CASE)
    private val crisisPattern =
        Regex(
            """(?<!:)(?<!\S)/crisis\b([^\n]*?)(?=(?<!:)(?<!\S)/(?:normal|exhaustion|unproductive|crisis)\b|$)""",
            RegexOption.IGNORE_CASE,
        )
    private val leadingWhitespacePattern = Regex("""^\s+""")

    fun parse(rawNoteText: String): StateCommandParseResult {
        if (rawNoteText.isBlank()) {
            return StateCommandParseResult(cleanedText = rawNoteText, detectedChange = null, allCommandsFound = emptyList())
        }

        val commands = mutableListOf<ParsedStateCommand>()
        val validRanges = mutableListOf<IntRange>()

        nonCrisisPattern.findAll(rawNoteText).forEach { match ->
            val type =
                when (match.groupValues[1].lowercase()) {
                    "normal" -> UserAwarenessStateType.NORMAL
                    "exhaustion" -> UserAwarenessStateType.EXHAUSTION
                    else -> UserAwarenessStateType.UNPRODUCTIVE
                }
            commands +=
                ParsedStateCommand(
                    rawCommand = match.value,
                    change = UserStateChange(type = type),
                    rangeStart = match.range.first,
                    rangeEndExclusive = match.range.last + 1,
                )
            validRanges += match.range
        }

        crisisPattern.findAll(rawNoteText).forEach { match ->
            val args = match.groupValues[1]
            val parsedCrisis = parseCrisisArgs(args) ?: return@forEach
            commands +=
                ParsedStateCommand(
                    rawCommand = match.value,
                    change = parsedCrisis,
                    rangeStart = match.range.first,
                    rangeEndExclusive = match.range.last + 1,
                )
            validRanges += match.range
        }

        val cleaned = cleanText(rawNoteText, validRanges)
        val detected = commands.maxByOrNull { it.rangeStart }?.change
        return StateCommandParseResult(cleanedText = cleaned, detectedChange = detected, allCommandsFound = commands.sortedBy { it.rangeStart })
    }

    private fun parseCrisisArgs(rawArgs: String): UserStateChange? {
        val normalized = rawArgs.trim()
        if (normalized.isEmpty()) {
            return UserStateChange(type = UserAwarenessStateType.CRISIS, crisisLevel = 1, label = null)
        }
        val tokens = normalized.split(Regex("""\s+"""))
        val first = tokens.firstOrNull().orEmpty()
        val level = first.toIntOrNull()
        if (level != null && level !in 1..3) {
            return null
        }

        val crisisLevel = level ?: 1
        val labelRaw =
            if (level != null) {
                normalized.removePrefix(first).trim()
            } else {
                normalized
            }
        val label = labelRaw.take(80).trim().ifBlank { null }
        return UserStateChange(type = UserAwarenessStateType.CRISIS, crisisLevel = crisisLevel, label = label)
    }

    private fun cleanText(
        raw: String,
        rangesToRemove: List<IntRange>,
    ): String {
        if (rangesToRemove.isEmpty()) return raw
        val merged = mergeRanges(rangesToRemove)
        val builder = StringBuilder(raw)
        for (range in merged.asReversed()) {
            builder.delete(range.first, range.last + 1)
        }
        val withoutCommands = builder.toString()
        val normalizedLines =
            withoutCommands.lines().map { line ->
                line
                    .replace(Regex("""[ \t]{2,}"""), " ")
                    .replace(leadingWhitespacePattern, "")
                    .trimEnd()
            }
        return normalizedLines.joinToString("\n").trim()
    }

    private fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val merged = mutableListOf<IntRange>()
        var current = sorted.first()
        for (next in sorted.drop(1)) {
            current =
                if (next.first <= current.last + 1) {
                    current.first..maxOf(current.last, next.last)
                } else {
                    merged += current
                    next
                }
        }
        merged += current
        return merged
    }
}
