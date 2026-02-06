package com.romankozak.forwardappmobile

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import timber.log.Timber
import java.io.File
import com.romankozak.forwardappmobile.logging

@HiltAndroidApp
class ForwardAppMobileApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

 override fun onCreate() {
    super.onCreate()

    // 1️⃣ ЛОГЕР ПЕРШИМ
    val logsDir = File(filesDir, "logs")

    Timber.plant(
        Timber.DebugTree(),          // Logcat
        CoroutineFileTree(logsDir)   // File + coroutines
    )

    Timber.i("Application onCreate started")

    // 2️⃣ ВСЕ ІНШЕ
    appScope.launch {
        runCatching {
            settingsRepository.featureTogglesFlow.first()
        }.onSuccess { toggles ->
            Timber.i("Feature toggles loaded")
            FeatureToggles.updateAll(toggles)
        }.onFailure {
            Timber.e(it, "Failed to load feature toggles")
        }
    }
   }
}
