package com.romankozak.forwardappmobile.ui.screens.commanddeck

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CommandDeckViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val sharedPreferences = application.getSharedPreferences("command_deck_prefs", Context.MODE_PRIVATE)

    fun isCategoryExpanded(categoryTitle: String): Boolean {
        return sharedPreferences.getBoolean(categoryTitle, false)
    }

    fun setCategoryExpanded(categoryTitle: String, isExpanded: Boolean) {
        sharedPreferences.edit().putBoolean(categoryTitle, isExpanded).apply()
    }
}
