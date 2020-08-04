package org.ngbp.jsonrpc4jtestharness.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import com.nextgenbroadcast.mobile.middleware.atsc3.Atsc3Module
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
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