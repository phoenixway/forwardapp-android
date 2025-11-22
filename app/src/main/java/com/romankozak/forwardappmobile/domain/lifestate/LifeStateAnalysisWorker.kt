package com.romankozak.forwardappmobile.domain.lifestate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.romankozak.forwardappmobile.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.romankozak.forwardappmobile.domain.lifestate.model.AiAnalysis
import java.io.File

@HiltWorker
class LifeStateAnalysisWorker
@AssistedInject
constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val aiAnalyzerService: AiAnalyzerService,
) : CoroutineWorker(appContext, workerParams) {

    private val cacheFile = File(appContext.filesDir, "life_state_analysis.json")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override suspend fun doWork(): Result {
        createNotificationChannel()
        showStatus("Life analysis is running…")
        return withContext(Dispatchers.IO) {
            val result = aiAnalyzerService.analyzeLifeState()
            result.fold(
                onSuccess = {
                    persistAnalysis(it)
                    showStatus("Life analysis ready")
                    Result.success(workDataOf(KEY_STATUS to STATUS_SUCCESS))
                },
                onFailure = { error ->
                    showStatus("Life analysis failed: ${error.message ?: "error"}")
                    Result.failure(workDataOf(KEY_STATUS to STATUS_FAILURE, KEY_ERROR to (error.message ?: "error")))
                },
            )
        }
    }

    private fun persistAnalysis(analysis: AiAnalysis) {
        runCatching {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(json.encodeToString(analysis))
        }
    }

    private fun showStatus(message: String) {
        val builder =
            NotificationCompat
                .Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("ForwardApp Life Analysis")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Life Analysis"
            val descriptionText = "Notifications for life analysis generation"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText }
            val notificationManager: NotificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "life_analysis_channel"
        private const val NOTIFICATION_ID = 4242
        const val KEY_STATUS = "life_analysis_status"
        const val KEY_ERROR = "life_analysis_error"
        const val STATUS_SUCCESS = "success"
        const val STATUS_FAILURE = "failure"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<LifeStateAnalysisWorker>().addTag(LIFE_ANALYSIS_TAG).build()
            WorkManager.getInstance(context).enqueue(request)
        }

        const val LIFE_ANALYSIS_TAG = "life_analysis_unique"
    }
}
