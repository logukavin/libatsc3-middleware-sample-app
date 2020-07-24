package org.ngbp.jsonrpc4jtestharness.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import org.ngbp.jsonrpc4jtestharness.App
import org.ngbp.jsonrpc4jtestharness.di.module.AndroidBindingModule
import org.ngbp.jsonrpc4jtestharness.di.module.AppBindingModule
import org.ngbp.jsonrpc4jtestharness.di.module.AppModule
import org.ngbp.jsonrpc4jtestharness.di.module.MiddlewareModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            AndroidInjectionModule::class,
            AppModule::class,
            AndroidBindingModule::class,
            MiddlewareModule::class,
            AppBindingModule::class
        ]
)
interface ApplicationComponent : AndroidInjector<App> {
    @Component.Factory
    interface Factory {
        fun create(@BindsInstance appContext: Application): ApplicationComponent
    }
}