package org.ngbp.jsonrpc4jtestharness.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.IRPCGateway
import org.ngbp.jsonrpc4jtestharness.gateway.rpc.RPCGatewayImpl
import org.ngbp.libatsc3.Atsc3Module
import javax.inject.Singleton

@Module
class MiddlewareModule {
    @Provides
    @Singleton
    internal fun provideAtsc3Module(context: Context): Atsc3Module {
        return Atsc3Module(context)
    }

    @Provides
    fun provideRPCGateway(serviceController: IServiceController, viewController: IViewController): IRPCGateway {
        return RPCGatewayImpl(serviceController, viewController, Dispatchers.Main, Dispatchers.IO)
    }
}