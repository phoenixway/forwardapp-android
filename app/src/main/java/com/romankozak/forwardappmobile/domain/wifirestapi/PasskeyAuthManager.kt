package com.romankozak.forwardappmobile.domain.wifirestapi

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.CreateCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthDebug"

@Singleton
class PasskeyAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)


    }

    suspend fun testPasskeySupport(): Boolean {
        return try {
            checkCredentialManagerCapabilities()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Passkey support test failed", e)
            false
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun checkCredentialManagerCapabilities(): Result<Unit> = runCatching {
        Log.d(TAG, "=== ДІАГНОСТИКА CREDENTIAL MANAGER ===")

        // 1. Перевірка біометричних можливостей
        val biometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d(TAG, "✅ Біометричні дані або PIN/Pattern доступні")
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.w(TAG, "❌ Біометричне обладнання недоступне")
                throw Exception("Пристрій не підтримує біометричну автентифікацію")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.w(TAG, "⚠️ Біометричне обладнання тимчасово недоступне")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.w(TAG, "⚠️ Біометричні дані не налаштовані. Перевіряємо PIN/Pattern...")
                // Можна продовжити, якщо є PIN/Pattern
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.w(TAG, "❌ Потрібне оновлення безпеки")
                throw Exception("Потрібне оновлення системи безпеки")
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                Log.w(TAG, "❓ Невідомий статус біометричних даних")
            }
        }

        // 2. Перевірка версії Android
        val sdkVersion = android.os.Build.VERSION.SDK_INT
        Log.d(TAG, "Android SDK версія: $sdkVersion")
        if (sdkVersion < 28) {
            Log.w(TAG, "⚠️ Android версія може не підтримувати Passkeys повноцінно")
        }

        // 3. Перевірка Google Play Services (якщо доступно)
        try {
            val gmsChecker = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val gmsStatus = gmsChecker.isGooglePlayServicesAvailable(context)
            when (gmsStatus) {
                com.google.android.gms.common.ConnectionResult.SUCCESS -> {
                    Log.d(TAG, "✅ Google Play Services доступні")
                }
                else -> {
                    Log.w(TAG, "⚠️ Google Play Services статус: $gmsStatus")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Не вдалося перевірити Google Play Services", e)
        }

        Log.d(TAG, "=== КІНЕЦЬ ДІАГНОСТИКИ ===")
    }

    suspend fun register(username: String, apiService: ApiService): Result<Unit> = runCatching {
        Log.d(TAG, "REGISTER {$username} started!")
        Log.d(TAG, "register: Початок реєстрації. URL сервера: 'http://192.168.0.106:8000/'")

        // Спочатку перевіряємо можливості системи
        checkCredentialManagerCapabilities().getOrThrow()

        Log.d(TAG, "register: Крок 1 - Отримання опцій з сервера...")

        val response = apiService.generateRegistrationOptions(UsernameRequest(username))
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Failed to get registration options from server: ${response.errorBody()?.string()}")
        }

        val requestJsonObject = response.body()!!
        val requestJson = requestJsonObject.toString()

        Log.d(TAG, "JSON, що передається в CredentialManager: $requestJson")
        Log.d(TAG, "register: Крок 2 - Опції отримано. Виклик системного діалогу...")

        try {
            // Використовуємо стандартний публічний конструктор
            val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(requestJson)

            val result = credentialManager.createCredential(context, createPublicKeyCredentialRequest)

            Log.d(TAG, "register: Крок 3 - Системний діалог завершено. Верифікація на сервері...")
            val responseJsonString = result.data.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
                ?: throw Exception("Registration response JSON is null")

            // Парсимо відповідь від Credential Manager
            val credentialJsonElement = json.parseToJsonElement(responseJsonString)
            val credentialMap = credentialJsonElement.jsonObject.toMutableMap()

            // Додаємо username до запиту верифікації
            credentialMap["username"] = json.parseToJsonElement("\"$username\"")

            val verificationResponse = apiService.verifyRegistration(credentialMap)
            if (!verificationResponse.isSuccessful || verificationResponse.body()?.verified != true) {
                throw Exception("Server verification failed: ${verificationResponse.errorBody()?.string()}")
            }

            Log.d(TAG, "register: Реєстрація успішно завершена!")
        } catch (e: CreateCredentialException) {
            Log.e(TAG, "register: CreateCredentialException - ${e.type}", e)

            // Обробляємо різні типи помилок на основі повідомлення
            val errorMessage = when {
                e.message?.contains("No create options available") == true -> {
                    "Пристрій не підтримує створення Passkeys. Переконайтесь що:\n" +
                            "• Налаштовано PIN, Pattern, Password або біометрію\n" +
                            "• Google Play Services оновлені\n" +
                            "• Тестуєте на реальному пристрої"
                }
                e.message?.contains("canceled") == true -> {
                    "Користувач скасував створення Passkey"
                }
                else -> {
                    "Помилка створення Passkey: ${e.message ?: "Невідома помилка"}"
                }
            }

            throw Exception(errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "register: ПОМИЛКА під час реєстрації", e)
            throw e
        }
    }

    suspend fun login(username: String, apiService: ApiService): Result<String> = runCatching {
        Log.d(TAG, "LOGIN {$username} started!")
        Log.d(TAG, "login: Крок 1 - Отримання опцій з сервера...")

        val response = apiService.generateAuthenticationOptions(UsernameRequest(username))
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Failed to get authentication options from server: ${response.errorBody()?.string()}")
        }

        val requestJsonObject = response.body()!!
        val requestJson = requestJsonObject.toString()

        Log.d(TAG, "login: Крок 2 - Опції отримано. Виклик системного діалогу...")

        try {
            val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(requestJson)
            val getCredentialRequest = GetCredentialRequest(listOf(getPublicKeyCredentialOption))
            val result = credentialManager.getCredential(context, getCredentialRequest)

            Log.d(TAG, "login: Крок 3 - Системний діалог завершено. Верифікація на сервері...")
            val responseJsonString = result.credential.data.getString("androidx.credentials.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON")
                ?: throw Exception("Authentication response JSON is null")

            // Парсимо відповідь від Credential Manager
            val credentialJsonElement = json.parseToJsonElement(responseJsonString)
            val credentialMap = credentialJsonElement.jsonObject.toMutableMap()

            // Додаємо username до запиту верифікації
            credentialMap["username"] = json.parseToJsonElement("\"$username\"")

            val authResponse = apiService.verifyAuthentication(credentialMap)
            if (!authResponse.isSuccessful || authResponse.body()?.verified != true || authResponse.body()?.access_token == null) {
                throw Exception("Server authentication failed: ${authResponse.errorBody()?.string()}")
            }

            Log.d(TAG, "login: Автентифікація успішно завершена!")
            return@runCatching authResponse.body()!!.access_token!!
        } catch (e: Exception) {
            Log.e(TAG, "login: ПОМИЛКА під час автентифікації.", e)
            throw e
        }
    }

    suspend fun testPasskeySupport(context: Context) {
        val manager = PasskeyAuthManager(context)
        manager.checkCredentialManagerCapabilities().onSuccess {
            Log.d("Test", "Passkeys можуть працювати ✅")
        }.onFailure {
            Log.e("Test", "Passkeys не підтримуються ❌", it)
        }
    }

}