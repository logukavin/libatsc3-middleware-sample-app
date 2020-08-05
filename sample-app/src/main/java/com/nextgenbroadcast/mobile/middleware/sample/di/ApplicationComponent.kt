package com.nextgenbroadcast.mobile.middleware.sample.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import com.nextgenbroadcast.mobile.middleware.di.MiddlewareBindingModule
import com.nextgenbroadcast.mobile.middleware.di.MiddlewareModule
import com.nextgenbroadcast.mobile.middleware.sample.App
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            AndroidInjectionModule::class,
            com.nextgenbroadcast.mobile.middleware.sample.di.module.AppModule::class,
            com.nextgenbroadcast.mobile.middleware.sample.di.module.AndroidBindingModule::class,
            MiddlewareModule::class,
            MiddlewareBindingModule::class
        ]
)
interface ApplicationComponent : AndroidInjector<App> {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance appContext: Application): ApplicationComponent
    }
}