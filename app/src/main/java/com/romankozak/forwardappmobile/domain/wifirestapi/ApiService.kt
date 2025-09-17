// ApiService.kt

package com.romankozak.forwardappmobile.domain.wifirestapi

import kotlinx.serialization.json.JsonObject // <-- ДОДАЙТЕ ЦЕЙ ІМПОРТ
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    // --- МЕТОДИ ДЛЯ ПЕРЕДАЧІ ФАЙЛІВ ---

    @Multipart
    @POST("/api/v1/files")
    suspend fun uploadFile(
        @Part("filename") filename: RequestBody,
        @Part content: MultipartBody.Part
    ): Response<Unit>

    @Multipart
    @POST("/api/v1/backlog")
    suspend fun uploadBacklog(
        @Part("filename") filename: RequestBody,
        @Part content: MultipartBody.Part
    ): Response<Unit>

    // --- МЕТОДИ ДЛЯ PASSKEY АВТЕНТИФІКАЦІЇ ---

    // ВИПРАВЛЕНО: Тип відповіді змінено на JsonObject
    @POST("/generate-registration-options")
    suspend fun generateRegistrationOptions(@Body body: UsernameRequest): Response<JsonObject>

    // ВИПРАВЛЕНО: Тип відповіді змінено на JsonObject
    @POST("/generate-authentication-options")
    suspend fun generateAuthenticationOptions(@Body body: UsernameRequest): Response<JsonObject>

    // ВИПРАВЛЕНО: Залишено тільки один правильний метод
    @POST("/verify-registration")
    suspend fun verifyRegistration(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<AuthResponse>

    // ВИПРАВЛЕНО: Залишено тільки один правильний метод
    @POST("/verify-authentication")
    suspend fun verifyAuthentication(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<AuthResponse>
}