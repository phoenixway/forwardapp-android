package com.romankozak.forwardappmobile.ui.screens.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.domain.wifirestapi.PasskeyAuthManager
import com.romankozak.forwardappmobile.domain.wifirestapi.RetrofitClient
import com.romankozak.forwardappmobile.domain.wifirestapi.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

private const val TAG = "AuthDebug"
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val app: Application,
    private val passkeyManager: PasskeyAuthManager,
    private val tokenManager: TokenManager,
    // Hilt автоматично надасть нам SavedStateHandle
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // --- ОСНОВНА ЗМІНА ---
    // Отримуємо URL з аргументів навігації. Він був переданий з попереднього екрану.
    private val serverUrl: String = savedStateHandle.get<String>("url")?.let {
        val decodedUrl = URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
        try {
            // Використовуємо стандартний клас URI для виділення "кореня" адреси
            val uri = URI(decodedUrl)
            // Формуємо базову адресу: схема://хост:порт/
            "${uri.scheme}://${uri.host}:${uri.port}/"
        } catch (e: Exception) {
            Log.e(TAG, "Не вдалося розпарсити URL: $decodedUrl", e)
            "" // Повертаємо порожній рядок у разі помилки
        }
    } ?: ""
    // --- КІНЕЦЬ ЗМІНИ ---

    fun register(username: String) {
        Log.d(TAG, "REGISTER $username} started!")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Тепер ми використовуємо serverUrl, отриманий з SavedStateHandle
            if (serverUrl.isBlank()) {
                Log.e(TAG, "register: Помилка - URL не було передано на екран автентифікації.")
                _uiState.update { it.copy(isLoading = false, error = "Помилка: URL сервера невідомий.") }
                return@launch
            }

            try {
                Log.d(TAG, "register: Початок реєстрації. URL сервера: '$serverUrl'")
                val api = RetrofitClient.getInstance(app, serverUrl)

                passkeyManager.register(username, api)
                    .onSuccess {
                        Log.d(TAG, "register: Успішно зареєстровано!")
                        _uiState.update { it.copy(isLoading = false, error = "Пристрій успішно зареєстровано!") }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "register: ПОМИЛКА під час реєстрації (onFailure).", error)
                        _uiState.update { it.copy(isLoading = false, error = "Помилка реєстрації: ${error.message}") }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "register: КРИТИЧНА ПОМИЛКА (catch).", e)
                _uiState.update { it.copy(isLoading = false, error = "Критична помилка: ${e.message}") }
            }
        }
    }

    fun login(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val isSupported = passkeyManager.testPasskeySupport()
            if (isSupported) {
                Log.d("Test", "Passkeys підтримуються ✅")
            } else {
                Log.d("Test", "Passkeys не підтримуються ❌")
            }
            if (serverUrl.isBlank()) {
                Log.e(TAG, "login: Помилка - URL не було передано на екран автентифікації.")
                _uiState.update { it.copy(isLoading = false, error = "Помилка: URL сервера невідомий.") }
                return@launch
            }

            try {
                Log.d(TAG, "login: Початок входу. URL сервера: '$serverUrl'")
                val api = RetrofitClient.getInstance(app, serverUrl)




                passkeyManager.login(username, api)
                    .onSuccess { token ->
                        Log.d(TAG, "login: Успішний вхід!")
                        tokenManager.saveToken(app, token)
                        _uiState.update { it.copy(isLoading = false, isAuthSuccess = true) }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "login: ПОМИЛКА під час входу (onFailure).", error)
                        _uiState.update { it.copy(isLoading = false, error = "Помилка входу: ${error.message}") }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "login: КРИТИЧНА ПОМИЛКА (catch).", e)
                _uiState.update { it.copy(isLoading = false, error = "Критична помилка: ${e.message}") }
            }
        }
    }


}