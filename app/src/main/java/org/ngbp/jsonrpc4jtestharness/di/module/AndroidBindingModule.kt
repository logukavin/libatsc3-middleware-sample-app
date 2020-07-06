package org.ngbp.jsonrpc4jtestharness.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.ngbp.jsonrpc4jtestharness.MainActivity

@Module
abstract class AndroidBindingModule {
    @ContributesAndroidInjector
    internal abstract fun mainActivityInjector(): MainActivity
}