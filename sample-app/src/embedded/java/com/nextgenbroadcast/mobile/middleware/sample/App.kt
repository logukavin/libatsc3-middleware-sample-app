package com.nextgenbroadcast.mobile.middleware.sample

import com.bugfender.sdk.Bugfender
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import com.nextgenbroadcast.mobile.middleware.sample.di.DaggerApplicationComponent
import com.nextgenbroadcast.mobile.middleware.service.EmbeddedAtsc3Service

class App : DaggerApplication() {

    override fun onCreate() {
        super.onCreate()

        Bugfender.init(this, BuildConfig.BugfenderKey, BuildConfig.DEBUG)
        Bugfender.enableCrashReporting()
        Bugfender.enableUIEventLogging(this)
        Bugfender.enableLogcatLogging() // optional, if you want logs automatically collected from logcat

        EmbeddedAtsc3Service.init()
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(this)
    }
}