package com.romankozak.forwardappmobile.core.sync

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.romankozak.forwardappmobile.core.data.interfaces.sync.IContentProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

class AndroidContentProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : IContentProvider {
        override fun readText(uriString: String): Result<String> {
            return runCatching {
                val uri = Uri.parse(uriString)
                Log.e("FullJsonImport", "contentProvider.readText uri=$uriString")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().use { it.readText() }
                } ?: throw IOException("Не вдалося відкрити файл")
            }.onSuccess { content ->
                Log.e("FullJsonImport", "contentProvider.readText success chars=${content.length} head=${content.take(80)}")
            }.onFailure { error ->
                Log.e("FullJsonImport", "contentProvider.readText failed uri=$uriString message=${error.message}", error)
            }
        }

        override fun saveFile(
            name: String,
            content: String,
        ): Result<Unit> {
            return runCatching {
                val values =
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ForwardApp")
                        }
                    }

                val collection =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Files.getContentUri("external")
                    }

                val uri =
                    context.contentResolver.insert(collection, values)
                        ?: throw IOException("Не вдалося створити запис у MediaStore")

                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(content.toByteArray())
                } ?: throw IOException("Не вдалося відкрити потік для запису")
            }
        }
    }
