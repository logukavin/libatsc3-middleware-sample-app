package com.nextgenbroadcast.mobile.middleware.controller.service

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.atsc3.MediaUrl
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl
import com.nextgenbroadcast.mobile.middleware.rpc.receiverQueryApi.model.AlertingRpcResponse

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val serviceGuideUrls: LiveData<List<SGUrl>?>
    val applications: LiveData<List<Atsc3Application>?>
    //TODO: hmm, this is not good
    val routeMediaUrl: LiveData<MediaUrl?>
    val alertList: LiveData<List<AlertingRpcResponse.Alert>>

    fun openRoute(source: IAtsc3Source): Boolean
    fun stopRoute()

    fun findServiceById(globalServiceId: String): AVService?
}