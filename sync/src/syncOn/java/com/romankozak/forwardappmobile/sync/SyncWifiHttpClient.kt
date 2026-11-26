package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SubjectConversionSnapshotAdapter
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.gson.gson

internal fun createSyncWifiHttpClient(): HttpClient = HttpClient(CIO) {
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 90_000
        requestTimeoutMillis = 90_000
    }
    install(ContentNegotiation) {
        gson {
            registerTypeAdapterFactory(SubjectConversionSnapshotAdapter())
            registerTypeAdapter(Long::class.java, LongDeserializer())
        }
    }
}
