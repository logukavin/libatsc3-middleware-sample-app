package com.nextgenbroadcast.mobile.middleware.di

import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers

@Module
class MiddlewareModule {
    @Provides
    fun provideRPCGateway(serviceController: IServiceController, viewController: IViewController): IRPCGateway {
        return RPCGatewayImpl(serviceController, viewController, Dispatchers.Main, Dispatchers.IO)
    }
}