package com.romankozak.forwardappmobile.core.data.interfaces.sync

// У вашому core-модулі або пакеті sync
// IContentProvider.kt
interface IContentProvider {
    fun readText(uriString: String): Result<String>
    fun saveFile(name: String, content: String): Result<Unit>
}