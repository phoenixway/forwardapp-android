package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler as AndroidAlarmScheduler
import com.romankozak.forwardappmobile.shared.features.reminders.domain.AlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmSchedulerModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler
}
