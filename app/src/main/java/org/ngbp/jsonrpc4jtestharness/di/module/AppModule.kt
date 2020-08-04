package org.ngbp.jsonrpc4jtestharness.di.module

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.RPCGatewayImpl
import javax.inject.Singleton

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application
    }

    @Provides
    fun rpcGatewayBinder(serviceController: IServiceController, viewController: IViewController): IRPCGateway {
        return RPCGatewayImpl(serviceController, viewController, Dispatchers.Main, Dispatchers.IO)
    }
}