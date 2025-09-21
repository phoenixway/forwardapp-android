// IoDispatcher.kt - Без змін, але додаємо ApplicationScope
package com.romankozak.forwardappmobile.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope