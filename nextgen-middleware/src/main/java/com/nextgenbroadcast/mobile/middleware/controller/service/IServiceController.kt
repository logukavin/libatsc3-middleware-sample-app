package com.nextgenbroadcast.mobile.middleware.controller.service

import androidx.lifecycle.LiveData
import com.nextgenbroadcast.mobile.core.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.core.serviceGuide.SGScheduleMap
import com.nextgenbroadcast.mobile.middleware.atsc3.source.IAtsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.serviceGuide.SGUrl

interface IServiceController : IReceiverPresenter, ISelectorPresenter {
    val schedule: LiveData<SGScheduleMap?>
    val serviceGuidUrls: LiveData<List<SGUrl>?>

    fun openRoute(source: IAtsc3Source): Boolean
    fun stopRoute()
}