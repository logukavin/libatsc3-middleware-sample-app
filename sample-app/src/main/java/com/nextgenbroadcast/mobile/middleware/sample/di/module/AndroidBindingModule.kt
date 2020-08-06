package com.nextgenbroadcast.mobile.middleware.sample.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import com.nextgenbroadcast.mobile.middleware.sample.MainActivity
import com.nextgenbroadcast.mobile.middleware.sample.NotificationReceiverActivity
import com.nextgenbroadcast.mobile.middleware.sample.useragent.UserAgentActivity
import com.nextgenbroadcast.mobile.middleware.sample.service.ForegroundRpcService

@Module
abstract class AndroidBindingModule {
    @ContributesAndroidInjector
    internal abstract fun mainActivityInjector(): MainActivity

    @ContributesAndroidInjector
    internal abstract fun foregroundRpcServiceInjector(): ForegroundRpcService

    @ContributesAndroidInjector
    internal abstract fun userAgentActivityInjector(): UserAgentActivity

    @ContributesAndroidInjector
    internal abstract fun withoutUIActivityInjector(): NotificationReceiverActivity
}