package org.ngbp.jsonrpc4jtestharness.di.module

import dagger.Binds
import dagger.Module
import org.ngbp.jsonrpc4jtestharness.controller.service.IServiceController
import org.ngbp.jsonrpc4jtestharness.controller.service.ServiceControllerImpl
import org.ngbp.jsonrpc4jtestharness.controller.view.IViewController
import org.ngbp.jsonrpc4jtestharness.controller.view.ViewControllerImpl
import org.ngbp.jsonrpc4jtestharness.core.repository.IRepository
import org.ngbp.jsonrpc4jtestharness.core.repository.RepositoryImpl
import org.ngbp.jsonrpc4jtestharness.gateway.web.IWebGateway
import org.ngbp.jsonrpc4jtestharness.gateway.web.WebGatewayImpl
import org.ngbp.jsonrpc4jtestharness.presentation.IMediaPlayerPresenter
import org.ngbp.jsonrpc4jtestharness.presentation.IReceiverPresenter
import org.ngbp.jsonrpc4jtestharness.presentation.ISelectorPresenter
import org.ngbp.jsonrpc4jtestharness.presentation.IUserAgentPresenter

@Module
abstract class AppBindingModule {
    @Binds
    abstract fun repositoryBinder(obj: RepositoryImpl): IRepository

    @Binds
    abstract fun dataControllerBinder(obj: ServiceControllerImpl): IServiceController

    @Binds
    abstract fun receiverPresenterBinder(obj: ServiceControllerImpl): IReceiverPresenter

    @Binds
    abstract fun selectorPresenterBinder(obj: ServiceControllerImpl): ISelectorPresenter

    @Binds
    abstract fun viewControllerBinder(obj: ViewControllerImpl): IViewController

    @Binds
    abstract fun userAgentPresenterBinder(obj: ViewControllerImpl): IUserAgentPresenter

    @Binds
    abstract fun mediaPlayerPresenterBinder(obj: ViewControllerImpl): IMediaPlayerPresenter

    @Binds
    abstract fun webGatewayBinder(obj: WebGatewayImpl): IWebGateway


}