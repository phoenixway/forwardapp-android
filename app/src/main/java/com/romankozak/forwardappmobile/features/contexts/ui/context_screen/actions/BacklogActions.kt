package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import android.util.Log
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.wifirestapi.FileDataRequest
import com.romankozak.forwardappmobile.domain.wifirestapi.RetrofitClient
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

class BacklogActions(
    private val listItemRepository: ListItemRepository,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        private const val TAG = "BacklogActions"
    }

    sealed class TransferResult {
        data class Message(val text: String) : TransferResult()
    }

    suspend fun move(
        currentContent: List<BacklogItemContent>,
        from: Int,
        to: Int,
    ): List<BacklogItemContent> {
        val currentList = moveInMemory(currentContent, from, to)
        persistBacklogOrder(currentList)
        return currentList
    }

    fun moveInMemory(
        currentContent: List<BacklogItemContent>,
        from: Int,
        to: Int,
    ): List<BacklogItemContent> {
        if (from !in currentContent.indices || to !in currentContent.indices || from == to) return currentContent
        val currentList = currentContent.toMutableList()
        val movedItem = currentList.removeAt(from)
        currentList.add(to, movedItem)
        return currentList
    }

    suspend fun moveToTop(
        currentContent: List<BacklogItemContent>,
        item: BacklogItemContent,
    ): List<BacklogItemContent> {
        val currentList = currentContent.toMutableList()
        val from = currentList.indexOf(item)
        if (from == -1) return currentContent
        val movedItem = currentList.removeAt(from)
        currentList.add(0, movedItem)
        persistBacklogOrder(currentList)
        return currentList
    }

    fun getBacklogAsMarkdown(content: List<BacklogItemContent>): String {
        val markdownBuilder = StringBuilder()
        content.forEach { item ->
            val line =
                when (item) {
                    is BacklogItemContent.GoalItem -> {
                        val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
                        "$checkbox ${item.goal.text}"
                    }

                    is BacklogItemContent.ContextLinkItem -> "- [С] ${item.project.name}"
                    is BacklogItemContent.LinkItem -> {
                        val displayName = item.link.linkData.displayName ?: item.link.linkData.target
                        "- [Л] [$displayName](${item.link.linkData.target})"
                    }

                    is BacklogItemContent.NoteItem -> "- [Н] ${item.note.title}"
                    is BacklogItemContent.NoteDocumentItem -> "- [К] ${item.document.name}"
                    is BacklogItemContent.JournalDocumentItem -> "- [Ж] ${item.document.name}"
                    is BacklogItemContent.MusicNoteItem -> "- [М] ${item.musicNote.name}"
                    is BacklogItemContent.ChecklistItem -> "- [Ч] ${item.checklist.name}"
                }
            markdownBuilder.appendLine(line)
        }
        return markdownBuilder.toString()
    }

    suspend fun transferBacklogToServer(
        projectName: String?,
        currentContent: List<BacklogItemContent>,
    ): TransferResult {
        val url = settingsRepository.getFastApiUrl().first()
        val markdownContent = getBacklogAsMarkdown(currentContent)
        val message =
            if (url.isNullOrBlank()) {
                "Server address is not available. Check settings."
            } else if (markdownContent.isBlank()) {
                "Беклог порожній. Нічого передавати."
            } else {
                try {
                    val filename = projectName ?: "backlog_export"
                    val requestBody = FileDataRequest(filename = filename, content = markdownContent)
                    Log.d(TAG, "transferBacklogToServer: Uploading to $url")
                    val response = RetrofitClient.getInstance(url).uploadFileAsJson(requestBody)
                    if (response.isSuccessful) {
                        "Беклог успішно передано"
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Невідома помилка"
                        "Помилка: ${response.code()} - $errorMsg"
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "transferBacklogToServer: network error", e)
                    "Помилка мережі: ${e.message}"
                } catch (e: HttpException) {
                    Log.e(TAG, "transferBacklogToServer: http error", e)
                    "Помилка HTTP: ${e.message()}"
                }
            }
        return TransferResult.Message(message)
    }

    suspend fun persistBacklogOrder(content: List<BacklogItemContent>) {
        val reorderedBacklogItems =
            content.mapIndexed { index, item ->
                item.backlogItem.copy(order = index.toLong())
            }
        listItemRepository.updateListItemsOrder(reorderedBacklogItems)
    }
}
