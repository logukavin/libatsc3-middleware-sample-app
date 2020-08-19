package com.nextgenbroadcast.mobile.middleware.sample.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import com.nextgenbroadcast.mobile.middleware.sample.MainActivity

@Module
abstract class AndroidBindingModule {
    @ContributesAndroidInjector
    internal abstract fun mainActivityInjector(): MainActivity
}