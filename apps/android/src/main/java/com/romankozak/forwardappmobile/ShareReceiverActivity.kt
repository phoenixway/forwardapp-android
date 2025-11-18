package com.romankozak.forwardappmobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

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

class ShareReceiverActivity : AppCompatActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val TAG = "ShareReceiverActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                Toast.makeText(this, "Forwarding to FAM...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Received shared text. Sending to hardcoded server address...")
                coroutineScope.launch {
                    // TODO: Make this configurable
                    val serverAddress = "http://192.168.0.15:8008/"
                    if (serverAddress.isBlank()) {
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
                val logging = HttpLoggingInterceptor()
                logging.setLevel(HttpLoggingInterceptor.Level.BODY)
                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(Gson()))
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
