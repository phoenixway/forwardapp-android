package com.romankozak.forwardappmobile

import android.app.Application

import com.romankozak.forwardappmobile.di.AppComponent

import com.romankozak.forwardappmobile.di.create

class ForwardAppMobileApplication : Application() {

    val appComponent: AppComponent by lazy {
        AppComponent::class.create().apply {
            application = this@ForwardAppMobileApplication
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
