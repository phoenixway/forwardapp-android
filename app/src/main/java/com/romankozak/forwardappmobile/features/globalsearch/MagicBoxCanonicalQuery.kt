package com.romankozak.forwardappmobile.features.globalsearch

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.SavedEntityQueries
import com.romankozak.forwardappmobile.data.entitybrowser.SavedEntityQuery
import com.romankozak.forwardappmobile.features.entitybrowser.*

enum class MagicBoxQueryIssue { INVALID_QUERY, SAVED_MISSING, SAVED_AMBIGUOUS, SAVED_UNAVAILABLE }

data class MagicBoxQueryResolution(
    val query: CanonicalEntityQuery? = null,
    val issue: MagicBoxQueryIssue? = null,
    val messages: List<String> = emptyList(),
    val choices: List<SavedEntityQuery> = emptyList(),
)

/** Saved syntax is standalone; the rest of the input is the exact display name (spaces allowed). */
fun resolveMagicBoxQuery(input: String, saved: SavedEntityQueries, selectedId: String? = null): MagicBoxQueryResolution {
    val value = input.trim()
    if (!value.startsWith("saved:", ignoreCase = true)) {
        val parsed = parseCanonicalEntityQuery(value)
        return if (parsed.isValid) MagicBoxQueryResolution(query = parsed.query)
        else MagicBoxQueryResolution(issue = MagicBoxQueryIssue.INVALID_QUERY, messages = parsed.errors)
    }
    if (saved.issue != null) return MagicBoxQueryResolution(issue = MagicBoxQueryIssue.SAVED_UNAVAILABLE, messages = listOf(saved.issue))
    val name = value.substringAfter(':').trim()
    val matches = saved.items.filter { it.name == name }
    if (name.isEmpty() || matches.size > 1) {
        val chosen = (if (name.isEmpty()) saved.items else matches).singleOrNull { it.id == selectedId }
        if (chosen != null) return decodeMagicBoxSaved(chosen)
        return MagicBoxQueryResolution(issue = MagicBoxQueryIssue.SAVED_AMBIGUOUS,
            messages = listOf("Оберіть збережений запит"), choices = if (name.isEmpty()) saved.items else matches)
    }
    val item = matches.singleOrNull()
        ?: return MagicBoxQueryResolution(issue = MagicBoxQueryIssue.SAVED_MISSING, messages = listOf("Збережений запит не знайдено: $name"))
    // A previously selected ID must never silently resolve to a different same-name query.
    if (selectedId != null && selectedId != item.id) return MagicBoxQueryResolution(
        issue = MagicBoxQueryIssue.SAVED_MISSING, messages = listOf("Обраний запит більше недоступний"))
    return decodeMagicBoxSaved(item)
}

private fun decodeMagicBoxSaved(item: SavedEntityQuery): MagicBoxQueryResolution {
    val decoded = item.decode()
    return if (decoded.query != null) MagicBoxQueryResolution(query = decoded.query)
    else MagicBoxQueryResolution(issue = MagicBoxQueryIssue.SAVED_UNAVAILABLE, messages = listOf(decoded.issue ?: "Запит недоступний"))
}

fun magicBoxCanonicalDestination(ref: CanonicalEntityRef): NavTarget = when (ref) {
    is CanonicalEntityRef.Orientation -> NavTarget.SemanticEntityEditor(ref.id)
    is CanonicalEntityRef.Aspect -> NavTarget.SemanticEntityEditor(ref.id)
    is CanonicalEntityRef.Workspace -> NavTarget.Workspace(ref.id)
}

fun hasCanonicalQueryOperators(input: String): Boolean = input.split(Regex("\\s+")).any { it.startsWith('#') || ':' in it }
