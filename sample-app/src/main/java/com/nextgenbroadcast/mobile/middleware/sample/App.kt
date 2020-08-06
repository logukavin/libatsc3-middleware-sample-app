package com.nextgenbroadcast.mobile.middleware.sample

import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import com.nextgenbroadcast.mobile.middleware.sample.di.DaggerApplicationComponent
import com.nextgenbroadcast.mobile.middleware.Atsc3ForegroundService

class App : DaggerApplication() {

    override fun onCreate() {
        super.onCreate()

        ContextCompat.startForegroundService(this, Intent(this, Atsc3ForegroundService::class.java).apply {
            action = Atsc3ForegroundService.ACTION_START
            putExtra("inputExtra", "Foreground RPC Service Example in Android")
        })
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(this)
    }
}