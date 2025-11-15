package com.romankozak.forwardappmobile.features.projectscreen.di

import com.romankozak.forwardappmobile.features.projectscreen.BacklogViewModel
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.AssistedFactory
import me.tatarka.inject.annotations.Provides

interface ProjectScreenModule {

    @AssistedFactory
    fun interface BacklogViewModelFactory {
        fun create(@Assisted projectId: String?): BacklogViewModel
    }


}
