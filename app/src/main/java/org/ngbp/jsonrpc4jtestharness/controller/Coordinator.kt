package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.ngbp.jsonrpc4jtestharness.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.libatsc3.Atsc3Module
import org.ngbp.libatsc3.entities.service.Service
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Coordinator @Inject constructor(
        private val atsc3Module: Atsc3Module
) : IReceiverController, IUserAgentController, IMediaPlayerController, IRPCController, Atsc3Module.Listener {

    private val _state = MutableLiveData<Atsc3Module.State>()
    private val _sltServices = MutableLiveData<List<SLSService>>()
    //private val _serviceMediaUri = MutableLiveData<Uri?>()

    val _appData = MutableLiveData<AppData?>()

    private val _rpmParams = MutableLiveData<RPMParams>()

    override var language: String = Locale.getDefault().language
    override var queryServiceId: String? =  "tag:sinclairplatform.com,2020:WZTV:2727" //TODO: remove after tests
    override var mediaUrl: String? = "http://127.0.0.1:8080/10.4/MPD.mpd" //TODO: remove after applying data source
    override var playbackState: PlaybackState = PlaybackState.IDLE

    override val state = Transformations.distinctUntilChanged(_state)
    override val sltServices = Transformations.distinctUntilChanged(_sltServices)
    override val appData = Transformations.distinctUntilChanged(_appData)
    override val rpmParams = Transformations.distinctUntilChanged(_rpmParams)

    init {
        atsc3Module.setListener(this)
    }

    override fun onStateChanged(state: Atsc3Module.State?) {
        if (state == Atsc3Module.State.IDLE) {
            reset()
        }

        _state.postValue(state)
    }

    override fun onServicesLoaded(services: List<Service?>) {
        val slsServices = services.filterNotNull()
                .map { SLSService(it.serviceId, it.shortServiceName, it.globalServiceId) }
        _sltServices.postValue(slsServices)
    }

    override fun onCurrentServiceHeldChanged(appContextId: String?, entryPage: String?) {
        _appData.postValue(AppData(appContextId, entryPage))
    }

    override fun updateViewPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
        _rpmParams.postValue(RPMParams(
                scaleFactor ?: 1.0,
                xPos?.toInt() ?: 0,
                yPos?.toInt() ?: 0
        ))
    }

    override fun openRoute(pcapFile: String): Boolean {
        return atsc3Module.openPcapFile(pcapFile)
    }

    override fun stopRoute() {
        atsc3Module.stop()
    }

    override fun closeRoute() {
        atsc3Module.close()
    }

    override fun selectService(service: SLSService) {
        val res = atsc3Module.selectService(service.id)
        if (res) {
            //TODO: should we use globalId or context from HELD?
            queryServiceId = service.globalId

            _appData.postValue(AppData(
                    atsc3Module.getSelectedServiceAppContextId(),
                    atsc3Module.getSelectedServiceEntryPoint()
            ))
        }
    }

    override fun rmpReset() {
        _rpmParams.postValue(RPMParams(100.0, 0, 0))
    }

    override fun rmpPlaybackChanged(state: PlaybackState) {
        playbackState = state
    }

    private fun reset() {
        queryServiceId = null
        _sltServices.postValue(emptyList())
        _appData.postValue(null)
        rmpReset()
    }
}
