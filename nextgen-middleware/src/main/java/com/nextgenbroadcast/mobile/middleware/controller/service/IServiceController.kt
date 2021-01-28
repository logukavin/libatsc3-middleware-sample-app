package com.nextgenbroadcast.mobile.middleware.controller.service

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap
import com.nextgenbroadcast.mobile.middleware.atsc3.entities.app.Atsc3Application
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val schedule: LiveData<SGScheduleMap?>
    val serviceGuideUrls: LiveData<List<SGUrl>?>
    val applications: LiveData<List<Atsc3Application>?>

    fun openRoute(source: IAtsc3Source): Boolean
    fun stopRoute()

    fun findServiceById(globalServiceId: String): AVService?
}