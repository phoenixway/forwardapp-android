// ApiModels.kt (рекомендовано перейменувати на ApiModels.kt)

package com.romankozak.forwardappmobile.domain.wifirestapi

import kotlinx.serialization.Serializable

/**
 * Модель даних для надсилання файлу на сервер у форматі JSON.
 * Відповідає Pydantic-моделі `FileData` на сервері FastAPI.
 */
@Serializable
data class FileDataRequest(
    val filename: String,
    val content: String
)