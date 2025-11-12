package com.romankozak.forwardappmobile.di

import me.tatarka.inject.annotations.Qualifier

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class IoDispatcher

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class MainDispatcher

@Qualifier
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class DefaultDispatcher

@Qualifier
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class ApplicationContext
