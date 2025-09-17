package com.romankozak.forwardappmobile.domain.wifirestapi

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor // <-- Переконайтесь, що цей імпорт є
import retrofit2.Retrofit

object RetrofitClient {

    fun getInstance(context: Context, baseUrl: String): ApiService {
        // Створюємо логгер
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Створюємо клієнт і додаємо логгер
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging) // <-- Цей рядок має бути тут
            .build()

        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient) // <-- Використовуємо клієнт з логгером
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}