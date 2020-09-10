package com.nextgenbroadcast.mobile.middleware

import com.nextgenbroadcast.mobile.core.model.AppData
import com.nextgenbroadcast.mobile.core.model.RPMParams
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService

interface OnIncomingDataListener {
    fun onReceiverState(receiverState: ReceiverState)
    fun onSLSServices(slsServices: List<SLSService>)
    fun onSelectSLSService(slsService: SLSService?)
    fun onAppData(appData: AppData?)
    fun onRPMParams(ppmParams: RPMParams)
    fun onRPMMediaUrl(rpmMediaUrl: String)
    fun onPlayerStatePause()
    fun onPlayerStateResume()
}