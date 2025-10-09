package com.romankozak.forwardappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Inject

// --- Data Models for Retrofit ---
data class FileData(val filename: String, val content: String)
data class ApiResponse(val status: String, val message: String)

// --- Retrofit Service Interface ---
interface FamApiService {
    @POST("api/v1/files")
    suspend fun sendFile(
        @Body fileData: FileData,
        @Query("subdir") subdir: String? = "shared-from-android"
    ): ApiResponse
}

@AndroidEntryPoint
class ShareReceiverActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val TAG = "ShareReceiverActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                Toast.makeText(this, "Forwarding to FAM...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Received shared text. Getting server address...")
                coroutineScope.launch {
                    val serverAddress = settingsRepository.getFastApiUrl().first()
                    if (serverAddress.isNullOrBlank()) {
                        Log.e(TAG, "Server address is not configured or not found.")
                        showToast("Server address not found. Please configure it in settings.")
                        finishAndRemoveTask()
                    } else {
                        Log.d(TAG, "Server address found: $serverAddress. Sending data...")
                        sendTextToServer(serverAddress, sharedText)
                    }
                }
            } else {
                finishAndRemoveTask()
            }
        } else {
            finishAndRemoveTask()
        }
    }

    private fun sendTextToServer(baseUrl: String, text: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(FamApiService::class.java)
                val filename = "note-${System.currentTimeMillis()}"
                val response = service.sendFile(FileData(filename = filename, content = text))

                Log.d(TAG, "Data sent successfully: ${response.message}")
                showToast(response.message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send data", e)
                showToast("Failed to send data: ${e.message}")
            } finally {
                finishAndRemoveTask()
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@ShareReceiverActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}