package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.data.repository.DirectionRepository

class DirectionActions(
    private val directionRepository: DirectionRepository,
) {
    sealed class OpenLinkedContextResult {
        data class Error(val message: String) : OpenLinkedContextResult()

        data class Navigate(
            val targetContextId: String,
            val originContextId: String,
        ) : OpenLinkedContextResult()
    }

    suspend fun updateDirectionItemText(
        item: DirectionItemEntity,
        text: String,
    ): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return "Напрямок не може бути порожнім."
        directionRepository.updateDirectionItem(item.copy(text = trimmed))
        return null
    }

    suspend fun deleteDirectionItem(itemId: String) {
        directionRepository.deleteDirectionItem(itemId)
    }

    suspend fun updateDirectionItemLink(
        currentItems: List<DirectionItemEntity>,
        itemId: String,
        linkedContextId: String?,
    ): Boolean {
        val item = currentItems.firstOrNull { it.id == itemId } ?: return false
        directionRepository.updateDirectionItem(item.copy(linkedContextId = linkedContextId))
        return true
    }

    suspend fun reorderDirectionItems(
        currentItems: List<DirectionItemEntity>,
        from: Int,
        to: Int,
    ): List<DirectionItemEntity>? {
        if (from !in currentItems.indices || to !in currentItems.indices || from == to) return null
        val mutable = currentItems.toMutableList()
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)

        val reordered =
            mutable.mapIndexed { index, item ->
                val newOrder = index + 1
                if (item.itemOrder != newOrder) item.copy(itemOrder = newOrder) else item
            }
        directionRepository.updateAll(reordered)
        return reordered
    }

    fun resolveOpenLinkedContext(
        targetContextId: String,
        currentContextId: String,
    ): OpenLinkedContextResult {
        if (targetContextId.isBlank() || targetContextId == currentContextId) {
            return OpenLinkedContextResult.Error("Це поточний контекст.")
        }
        return OpenLinkedContextResult.Navigate(
            targetContextId = targetContextId,
            originContextId = currentContextId,
        )
    }
}
