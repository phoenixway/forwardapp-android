package com.romankozak.forwardappmobile.core.storage

import android.content.Context
import android.os.Environment
import java.io.File

fun Context.getDocumentsLogsDir(): File {
    val docsDir =
        File(
            Environment.getExternalStorageDirectory(),
            "Documents/ForwardApp/logs",
        )

    if (!docsDir.exists()) {
        docsDir.mkdirs()
    }

    return docsDir
}
