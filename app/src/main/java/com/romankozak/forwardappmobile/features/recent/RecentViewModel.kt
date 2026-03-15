package com.romankozak.forwardappmobile.features.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FLOW_STOP_TIMEOUT_MILLIS = 5000L
private const val RECENT_ITEMS_LIMIT = 100

@HiltViewModel
class RecentViewModel
    @Inject
    constructor(
        private val recentItemsRepository: RecentItemsRepository,
    ) : ViewModel() {
        val recentItems =
            recentItemsRepository.getRecentItems(RECENT_ITEMS_LIMIT)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS),
                    emptyList(),
                )

        fun onPinClick(item: RecentItem) {
            viewModelScope.launch {
                recentItemsRepository.updateRecentItem(item.copy(isPinned = !item.isPinned))
            }
        }
    }
