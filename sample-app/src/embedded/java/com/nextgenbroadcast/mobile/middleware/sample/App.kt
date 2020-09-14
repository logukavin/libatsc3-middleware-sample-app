package com.nextgenbroadcast.mobile.middleware.sample

import android.util.Log
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import com.nextgenbroadcast.mobile.middleware.sample.di.DaggerApplicationComponent
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service

class App : DaggerApplication() {

    override fun onCreate() {
        super.onCreate()
        Log.d("TEST", "EmbeddedAtsc3Service.init()")
        EmbeddedAtsc3Service.init()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(this)
    }
}