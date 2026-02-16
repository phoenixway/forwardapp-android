package com.romankozak.forwardappmobile.features.userawareness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.UserAwarenessRepository
import com.romankozak.forwardappmobile.domain.userawareness.UserAwarenessStateType
import com.romankozak.forwardappmobile.domain.userawareness.UserStateInterval
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UserAwarenessViewModel
    @Inject
    constructor(
        private val repository: UserAwarenessRepository,
    ) : ViewModel() {
        init {
            viewModelScope.launch {
                repository.ensureDefaultState()
            }
        }

        val activeState: StateFlow<UserStateInterval?> =
            repository
                .observeActiveState()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        fun setNormal() {
            viewModelScope.launch {
                repository.setStateManual(UserAwarenessStateType.NORMAL)
            }
        }

        fun setExhaustion() {
            viewModelScope.launch {
                repository.setStateManual(UserAwarenessStateType.EXHAUSTION)
            }
        }

        fun setUnproductive() {
            viewModelScope.launch {
                repository.setStateManual(UserAwarenessStateType.UNPRODUCTIVE)
            }
        }

        fun setCrisis(
            level: Int,
            label: String?,
        ) {
            viewModelScope.launch {
                repository.setStateManual(
                    type = UserAwarenessStateType.CRISIS,
                    level = level,
                    label = label,
                )
            }
        }
    }
