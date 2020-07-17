package org.ngbp.jsonrpc4jtestharness.di.module

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.ngbp.jsonrpc4jtestharness.MainActivity
import org.ngbp.jsonrpc4jtestharness.UserAgentActivity
import org.ngbp.jsonrpc4jtestharness.NotificationReceiverActivity
import org.ngbp.jsonrpc4jtestharness.service.ForegroundRpcService

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