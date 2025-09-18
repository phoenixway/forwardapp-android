// ApiService.kt

package com.romankozak.forwardappmobile.domain.wifirestapi

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    /**
     * Надсилає файл (наприклад, беклог) на сервер у вигляді JSON.
     * @param body Об'єкт, що містить ім'я файлу та його вміст.
     */
    @POST("/api/v1/files")
    suspend fun uploadFileAsJson(@Body body: FileDataRequest): Response<Unit>

    // Старі методи для Passkey та multipart-завантаження видалено.
    // Старий метод для /api/v1/backlog також видалено, оскільки нова логіка
    // використовує універсальний ендпоінт /api/v1/files.
}