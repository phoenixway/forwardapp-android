package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardActions(
    private val application: Application,
) {
    fun copy(
        text: String,
        label: String,
    ) {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
