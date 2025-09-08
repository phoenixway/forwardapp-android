package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.romankozak.forwardappmobile.domain.RoleFile
import com.romankozak.forwardappmobile.domain.RoleFolder
import com.romankozak.forwardappmobile.domain.RoleItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RolesRepository"

@Singleton
class RolesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    val rolesHierarchyFlow: Flow<List<RoleItem>> = settingsRepository.rolesFolderUriFlow
        .flatMapLatest { uriString ->
            flow {
                if (uriString.isBlank()) {
                    emit(emptyList())
                } else {
                    try {
                        val uri = Uri.parse(uriString)
                        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                            uri,
                            DocumentsContract.getTreeDocumentId(uri)
                        )
                        val hierarchy = scanDirectory(childrenUri, uri.path ?: "roles")
                        emit(hierarchy)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read roles from URI: $uriString", e)
                        emit(emptyList())
                    }
                }
            }
        }
        .flowOn(Dispatchers.IO)


    private fun scanDirectory(directoryUri: Uri, currentPath: String): List<RoleItem> {
        val children = mutableListOf<RoleItem>()
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        try {
            contentResolver.query(directoryUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                    val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, docId)
                    val newPath = "$currentPath/$name"

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, docId)
                        children.add(RoleFolder(name, newPath, scanDirectory(childrenUri, newPath)))
                    } else if (name.endsWith(".md") || name.endsWith(".txt")) {
                        // --- ПОЧАТОК ЗМІНИ: Передаємо ім'я файлу в парсер ---
                        parseRoleFile(childUri, newPath, name)?.let { children.add(it) }
                        // --- КІНЕЦЬ ЗМІНИ ---
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: $directoryUri", e)
        }

        return children.sortedWith(compareBy({ !it.isFolder }, { it.name }))
    }

    // --- ПОЧАТОК ЗМІНИ: Повністю оновлена логіка парсингу файлу ---
    private fun parseRoleFile(fileUri: Uri, path: String, fileName: String): RoleFile? {
        return try {
            val content = context.contentResolver.openInputStream(fileUri)?.bufferedReader().use { it?.readText() }
            if (content.isNullOrBlank()) {
                return null // Ігноруємо порожні файли
            }

            val lines = content.lines()
            val firstLine = lines.first()

            if (firstLine.startsWith("#")) {
                // Випадок 1: Заголовок існує. Використовуємо його для назви.
                val roleName = firstLine.removePrefix("#").trim()
                val prompt = lines.drop(1).joinToString("\n").trim()
                RoleFile(roleName, path, prompt)
            } else {
                // Випадок 2: Заголовка немає. Використовуємо назву файлу.
                val roleName = fileName.removeSuffix(".md").removeSuffix(".txt")
                val prompt = content.trim()
                RoleFile(roleName, path, prompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing role file: $fileUri", e)
            null
        }
    }
    // --- КІНЕЦЬ ЗМІНИ ---
}