package com.romankozak.forwardappmobile.shared.features.search.di

import com.romankozak.forwardappmobile.di.IoDispatcher
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.search.data.repository.SearchRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.search.domain.repository.SearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface SearchModule {
    @Provides
    fun provideSearchRepository(
        db: ForwardAppDatabase,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): SearchRepository = SearchRepositoryImpl(db, dispatcher)
}
