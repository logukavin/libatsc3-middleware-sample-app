package com.nextgenbroadcast.mobile.middleware.sample.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import com.nextgenbroadcast.mobile.middleware.sample.App
import com.nextgenbroadcast.mobile.middleware.sample.di.module.AndroidBindingModule
import com.nextgenbroadcast.mobile.middleware.sample.di.module.AppModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            AndroidInjectionModule::class,
            AppModule::class,
            AndroidBindingModule::class
        ]
)
interface ApplicationComponent : AndroidInjector<App> {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance appContext: Application): ApplicationComponent
    }
}