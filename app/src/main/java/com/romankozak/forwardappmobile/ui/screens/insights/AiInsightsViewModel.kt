package com.romankozak.forwardappmobile.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Placeholder definitions for AiMessage and MessageType
/*data class AiMessage(
    val id: String,
    val content: String,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val type: MessageType = MessageType.INFO // Default type
)

enum class MessageType {
    INFO, WARNING, ERROR, MOTIVATION, JOKE
}*/

enum class FilterType {
    ALL,
    UNREAD,
    FAVORITES
}

data class AiInsightsState(
    val messages: List<AiMessage> = emptyList(),
    val filterType: FilterType = FilterType.ALL,
    val messageTypeFilter: MessageType? = null
)

class AiInsightsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiInsightsState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }

    fun onMessageClicked(message: AiMessage) {
        _uiState.update {
            val newMessages = it.messages.map {
                if (it == message) {
                    it.copy(isRead = true)
                } else {
                    it
                }
            }
            it.copy(messages = newMessages)
        }
    }

    fun onFavoriteClicked(message: AiMessage) {
        _uiState.update {
            val newMessages = it.messages.map {
                if (it == message) {
                    it.copy(isFavorite = !it.isFavorite)
                } else {
                    it
                }
            }
            it.copy(messages = newMessages)
        }
    }

    fun onFilterChanged(filterType: FilterType) {
        _uiState.update { it.copy(filterType = filterType) }
    }

    fun onMessageTypeFilterChanged(messageType: MessageType?) {
        _uiState.update { it.copy(messageTypeFilter = messageType) }
    }

    fun onDeleteMessage(message: AiMessage) {
        _uiState.update {
            val newMessages = it.messages.filter { it.id != message.id }
            it.copy(messages = newMessages)
        }
    }
}
