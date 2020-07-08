package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.jsonrpc4jtestharness.rpc.manager.RPCManager
import org.ngbp.libatsc3.Atsc3Module
import org.ngbp.libatsc3.ndk.a331.Service
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

    private val _rpmParams = MutableLiveData<RPMParams>()

    override val state = Transformations.distinctUntilChanged(_state)
    override val sltServices = Transformations.distinctUntilChanged(_sltServices)
    override val rpmParams = Transformations.distinctUntilChanged(_rpmParams)

    init {
        atsc3Module.setListener(this)
        rpcManager.setCallback(this)
    }

    override fun onStateChanged(state: Atsc3Module.State?) {
        _state.postValue(state)
    }

    override fun onServicesLoaded(services: List<Service?>) {
        val slsServices = services.filterNotNull()
                .map { SLSService(it.shortServiceName, it.globalServiceId) }
        _sltServices.postValue(slsServices)

        //TODO: move somewhere else
        services.firstOrNull { service -> "WZTV" == service?.shortServiceName }?.let { service ->
            atsc3Module.selectService(service)
            rpcManager.queryServiceId = service.globalServiceId
        }
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
}
