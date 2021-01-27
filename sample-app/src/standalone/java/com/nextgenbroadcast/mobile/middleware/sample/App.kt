package com.nextgenbroadcast.mobile.middleware.sample

import androidx.work.Configuration
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import com.nextgenbroadcast.mobile.middleware.sample.di.DaggerApplicationComponent

class App : DaggerApplication(), Configuration.Provider {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
                .setDefaultProcessName("${BuildConfig.APPLICATION_ID}:atsc3Service")
                .build()
    }
}