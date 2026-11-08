package com.romankozak.forwardappmobile.features.workspace

import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Transient presentation intent only; H1 remains the sole paste/move authority. */
sealed interface NavigationClipboardEntry {
    val token: String
    data class Copy(val target: CanonicalEntityRef, override val token: String = UUID.randomUUID().toString()) : NavigationClipboardEntry
    data class Cut(val node: H1TypedNode, override val token: String = UUID.randomUUID().toString()) : NavigationClipboardEntry
}

@Singleton
class NavigationClipboard @Inject constructor() {
    private val mutableEntry = MutableStateFlow<NavigationClipboardEntry?>(null)
    val entry = mutableEntry.asStateFlow()
    fun copy(target: CanonicalEntityRef) { mutableEntry.value = NavigationClipboardEntry.Copy(target) }
    fun cut(node: H1TypedNode) { mutableEntry.value = NavigationClipboardEntry.Cut(node) }
    fun clearIf(expected: NavigationClipboardEntry) { mutableEntry.compareAndSet(expected, null) }
    fun clear() { mutableEntry.value = null }
}
