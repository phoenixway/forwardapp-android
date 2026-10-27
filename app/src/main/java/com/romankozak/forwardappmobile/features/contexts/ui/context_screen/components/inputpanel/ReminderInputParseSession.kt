package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/** One exact-text parse (including a negative result), shared by preview and submission. */
internal class ReminderInputParseSession(
    private val scope: CoroutineScope,
    private val parser: ReminderParser,
    private val timeoutMs: Long,
) {
    private var text: String? = null
    private var pending: Deferred<ReminderParseResult>? = null

    @Synchronized
    fun selectText(value: String) {
        if (text != value) clear()
        text = value
    }

    @Synchronized
    fun preview(value: String): Deferred<ReminderParseResult> {
        selectText(value)
        return pending?.takeUnless { it.isCancelled } ?: scope.async(Dispatchers.IO) {
            parser.parseWithTimeout(value, timeoutMs)
        }.also { pending = it }
    }

    /** Editing the next draft must not cancel the parse belonging to the submitted Goal. */
    @Synchronized
    fun takeForSubmission(value: String): Deferred<ReminderParseResult> = preview(value).also {
        pending = null
        text = null
    }

    @Synchronized
    fun clear() {
        pending?.cancel()
        pending = null
        text = null
    }
}
