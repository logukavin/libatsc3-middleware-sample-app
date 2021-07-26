package com.nextgenbroadcast.mobile.middleware.dev.service.binder

import com.nextgenbroadcast.mobile.middleware.dev.presentation.IControllerPresenter

interface IServiceBinder {
    val controllerPresenter: IControllerPresenter?
}