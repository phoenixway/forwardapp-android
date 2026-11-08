package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import kotlinx.coroutines.CancellationException

/** Editor-only state: persist exact unlink before removing the draft row. Never saves other fields. */
internal class DayFocusLinkRemoval {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun remove(
        link: RelatedLink,
        persist: (suspend (RelatedLink) -> Unit)?,
        applyToDraft: (RelatedLink) -> Unit,
    ) {
        if (busy) return
        busy = true
        error = null
        try {
            persist?.invoke(link)
            applyToDraft(link)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            error = failure.message ?: "Не вдалося прибрати посилання"
        } finally {
            busy = false
        }
    }
}
