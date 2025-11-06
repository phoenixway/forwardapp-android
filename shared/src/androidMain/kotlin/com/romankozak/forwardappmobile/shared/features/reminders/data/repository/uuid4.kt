package com.romankozak.forwardappmobile.shared.features.reminders.data.repository

import java.util.UUID

actual fun uuid4(): String = UUID.randomUUID().toString()
