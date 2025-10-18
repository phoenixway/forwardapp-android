package com.romankozak.forwardappmobile.ui.screens.settings.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns

fun getFileName(uri: Uri, context: Context): String {
    var fileName: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
    } catch (e: Exception) {
        fileName = uri.lastPathSegment
    }
    return fileName ?: "Unknown file"
}

fun getFolderName(uri: Uri, context: Context): String = 
    try {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        context.contentResolver.query(docUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                uri.lastPathSegment ?: "Selected Folder"
            }
        } ?: uri.toString()
    } catch (e: Exception) {
        uri.lastPathSegment ?: "Selected Folder"
    }
