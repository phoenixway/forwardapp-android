package com.romankozak.forwardappmobile.di

import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Singleton

// Android-specific scope must live in Android app module to avoid duplicates
