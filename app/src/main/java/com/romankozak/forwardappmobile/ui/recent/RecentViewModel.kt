package com.romankozak.forwardappmobile.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val recentItemsRepository: RecentItemsRepository
) : ViewModel() {

    val recentItems = recentItemsRepository.getRecentItems(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onPinClick(item: RecentItem) {
        viewModelScope.launch {
            recentItemsRepository.updateRecentItem(item.copy(isPinned = !item.isPinned))
        }
    }
}