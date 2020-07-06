package org.ngbp.jsonrpc4jtestharness

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import org.ngbp.jsonrpc4jtestharness.di.DaggerApplicationComponent

class App : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.factory().create(this)
    }
}