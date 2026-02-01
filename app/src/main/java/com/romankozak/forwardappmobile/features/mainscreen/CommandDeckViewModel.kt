import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.domain.lifecontext.StartContextTrackingUseCase
import com.romankozak.forwardappmobile.domain.lifecontext.SubmitContextInputUseCase
import com.romankozak.forwardappmobile.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommandDeckViewModel
    @Inject
    constructor(
        private val application: Application,
        private val submitContextInputUseCase: SubmitContextInputUseCase,
        private val startContextTrackingUseCase: StartContextTrackingUseCase,
        private val syncRepository: SyncRepository,
    ) : ViewModel() {
        private val sharedPreferences = application.getSharedPreferences("command_deck_prefs", Context.MODE_PRIVATE)

        private val _isContextInputVisible = MutableStateFlow(false)
        val isContextInputVisible: StateFlow<Boolean> = _isContextInputVisible.asStateFlow()

        private val _contextInputText = MutableStateFlow("")
        val contextInputText: StateFlow<String> = _contextInputText.asStateFlow()

        fun isCategoryExpanded(categoryTitle: String): Boolean {
            return sharedPreferences.getBoolean(categoryTitle, false)
        }

        fun setCategoryExpanded(
            categoryTitle: String,
            isExpanded: Boolean,
        ) {
            sharedPreferences.edit().putBoolean(categoryTitle, isExpanded).apply()
        }

        fun openContextInput() {
            _isContextInputVisible.value = true
        }

        fun closeContextInput() {
            _isContextInputVisible.value = false
        }

        fun onContextInputChange(text: String) {
            _contextInputText.value = text
        }

        fun clearContextInput() {
            _contextInputText.value = ""
        }

        fun submitContextInput() {
            val text = _contextInputText.value.trim()
            if (text.isEmpty()) {
                closeContextInput()
                return
            }
            viewModelScope.launch {
                submitContextInputUseCase(text)
                clearContextInput()
                closeContextInput()
            }
        }

        fun startContextTracking() {
            val text = _contextInputText.value.trim()
            if (text.isEmpty()) {
                closeContextInput()
                return
            }
            viewModelScope.launch {
                startContextTrackingUseCase(text)
                clearContextInput()
                closeContextInput()
            }
        }

        fun onEvent(event: CommandDeckEvent) {
            when (event) {
                CommandDeckEvent.ExportToFile -> {
                    viewModelScope.launch {
                        syncRepository.exportDatabaseToFile().onFailure {
                            // TODO: Show error message
                        }
                    }
                }
                is CommandDeckEvent.ImportFromFileRequest -> {
                    viewModelScope.launch {
                        syncRepository.importDatabaseFromFile(Uri.parse(event.fileUri)).onFailure {
                            // TODO: Show error message
                        }
                    }
                }
                CommandDeckEvent.ExportAttachments -> {
                    viewModelScope.launch {
                        syncRepository.exportAttachmentsToFile().onFailure {
                            // TODO: Show error message
                        }
                    }
                }
                is CommandDeckEvent.ImportAttachmentsFromFile -> {
                    viewModelScope.launch {
                        syncRepository.importAttachmentsFromFile(Uri.parse(event.fileUri)).onFailure {
                            // TODO: Show error message
                        }
                    }
                }
                is CommandDeckEvent.WifiPush -> {
                    viewModelScope.launch {
                        syncRepository.pushChangesToWifiServer(event.host).onFailure {
                            // TODO: Show error message
                        }
                    }
                }
            }
        }
    }

sealed interface CommandDeckEvent {
    object ExportToFile : CommandDeckEvent
    data class ImportFromFileRequest(val fileUri: String) : CommandDeckEvent
    object ExportAttachments : CommandDeckEvent
    data class ImportAttachmentsFromFile(val fileUri: String) : CommandDeckEvent
    data class WifiPush(val host: String) : CommandDeckEvent
}
