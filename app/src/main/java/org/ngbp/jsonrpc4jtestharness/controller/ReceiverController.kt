package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.jsonrpc4jtestharness.rpc.manager.RPCManager
import org.ngbp.libatsc3.Atsc3Module
import org.ngbp.libatsc3.entities.service.Service
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiverController @Inject constructor(
        private val rpcManager: RPCManager,
        private val atsc3Module: Atsc3Module
) : IReceiverController, Atsc3Module.Listener, RPCManager.IReceiverCallback {

    private val _state = MutableLiveData<Atsc3Module.State>()
    private val _sltServices = MutableLiveData<List<SLSService>>()
    //private val _serviceMediaUri = MutableLiveData<Uri?>()

    val _appData = MutableLiveData<AppData?>()

    private val _rpmParams = MutableLiveData<RPMParams>()

    override val state = Transformations.distinctUntilChanged(_state)
    override val sltServices = Transformations.distinctUntilChanged(_sltServices)
    override val appData = Transformations.distinctUntilChanged(_appData)
    override val rpmParams = Transformations.distinctUntilChanged(_rpmParams)
    override val playerState = MutableLiveData<Int>()

    init {
        atsc3Module.setListener(this)
        rpcManager.setCallback(this)
        playerState.observeForever {
            rpcManager.playbackState = it
        }
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

    override fun updateViewPosition(scaleFactor: Double, xPos: Double, yPos: Double) {
        _rpmParams.postValue(RPMParams(scaleFactor, xPos.toInt(), yPos.toInt()))
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
            rpcManager.queryServiceId = service.globalId

            _appData.postValue(AppData(
                    atsc3Module.getSelectedServiceAppContextId(),
                    atsc3Module.getSelectedServiceEntryPoint()
            ))
        }
    }

    override fun resetRMP() {
        _rpmParams.postValue(RPMParams(100.0, 0, 0))
    }

    private fun reset() {
        rpcManager.queryServiceId = null
        _sltServices.postValue(emptyList())
        _appData.postValue(null)
        resetRMP()
    }
}
