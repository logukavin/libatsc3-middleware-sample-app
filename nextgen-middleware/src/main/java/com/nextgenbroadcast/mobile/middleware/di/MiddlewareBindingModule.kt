package com.nextgenbroadcast.mobile.middleware.di

import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.service.ServiceControllerImpl
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.controller.view.ViewControllerImpl
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.IRPCGateway
import com.nextgenbroadcast.mobile.middleware.gateway.rpc.RPCGatewayImpl
import com.nextgenbroadcast.mobile.middleware.gateway.web.IWebGateway
import com.nextgenbroadcast.mobile.middleware.gateway.web.WebGatewayImpl
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter
import com.nextgenbroadcast.mobile.middleware.repository.IRepository
import com.nextgenbroadcast.mobile.middleware.repository.RepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class MiddlewareBindingModule {
    @Binds
    internal abstract fun repositoryBinder(obj: RepositoryImpl): IRepository

    @Binds
    internal abstract fun dataControllerBinder(obj: ServiceControllerImpl): IServiceController

    @Binds
    internal abstract fun receiverPresenterBinder(obj: ServiceControllerImpl): IReceiverPresenter

    @Binds
    internal abstract fun selectorPresenterBinder(obj: ServiceControllerImpl): ISelectorPresenter

    @Binds
    internal abstract fun viewControllerBinder(obj: ViewControllerImpl): IViewController

    @Binds
    internal abstract fun userAgentPresenterBinder(obj: ViewControllerImpl): IUserAgentPresenter

    @Binds
    internal abstract fun mediaPlayerPresenterBinder(obj: ViewControllerImpl): IMediaPlayerPresenter

    @Binds
    internal abstract fun webGatewayBinder(obj: WebGatewayImpl): IWebGateway

}