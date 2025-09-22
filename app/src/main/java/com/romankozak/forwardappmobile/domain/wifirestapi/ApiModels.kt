

package com.romankozak.forwardappmobile.domain.wifirestapi

import kotlinx.serialization.Serializable


@Serializable
data class FileDataRequest(
    val filename: String,
    val content: String,
)
