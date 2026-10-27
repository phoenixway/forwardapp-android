package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** One settings store instance, shared by settings backup/Restore and query preferences. */
internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
