package com.romankozak.forwardappmobile.domain.aichat

import android.app.Service
import android.content.Intent
import android.os.IBinder

class GenerationService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
