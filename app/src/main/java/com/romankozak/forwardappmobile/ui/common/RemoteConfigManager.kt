package com.romankozak.forwardappmobile.ui.common

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romankozak.forwardappmobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val gson: Gson,
    ) {
        private val tag = "RemoteConfigManager"

        private val remoteConfig: FirebaseRemoteConfig? by lazy {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.w(tag, "Firebase is not initialized, Remote Config will use local defaults")
                null
            } else {
                FirebaseRemoteConfig.getInstance().apply {
                    val configSettings =
                        FirebaseRemoteConfigSettings.Builder()
                            .setMinimumFetchIntervalInSeconds(3600)
                            .build()
                    setConfigSettingsAsync(configSettings)
                    setDefaultsAsync(R.xml.remote_config_defaults)
                }
            }
        }

        suspend fun fetchAndActivate() {
            remoteConfig?.fetchAndActivate()?.await()
        }

        fun getIconMappings(): Map<String, List<String>> {
            val json = remoteConfig?.getString("hardcoded_icons").orEmpty()
            return if (json.isNotBlank() && json != "{}") {
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                gson.fromJson(json, type)
            } else {
                getLocalIconMappings()
            }
        }

        private fun getLocalIconMappings(): Map<String, List<String>> {
            val json =
                context.resources.openRawResource(R.raw.default_icons)
                    .bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            return gson.fromJson(json, type)
        }
    }
