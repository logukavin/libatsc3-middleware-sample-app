package org.ngbp.jsonrpc4jtestharness.controller

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.ngbp.jsonrpc4jtestharness.controller.media.IObservablePlayer
import org.ngbp.jsonrpc4jtestharness.controller.media.PlayerStateRegistry
import org.ngbp.jsonrpc4jtestharness.controller.model.AppData
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState
import org.ngbp.jsonrpc4jtestharness.controller.model.RPMParams
import org.ngbp.jsonrpc4jtestharness.controller.model.SLSService
import org.ngbp.libatsc3.Atsc3Module
import org.ngbp.libatsc3.entities.service.Service
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

//TODO: extract controllers as separate classes
@Singleton
class Coordinator @Inject constructor(
        private val atsc3Module: Atsc3Module
) : IReceiverController, IUserAgentController, IMediaPlayerController, IRPCController, Atsc3Module.Listener {

    // Receiver Controller
    override val receiverState = MutableLiveData<Atsc3Module.State>()
    override val selectedService = MutableLiveData<SLSService>()

    // Media Player Controller
    private var rmpListeners = PlayerStateRegistry()

    override val rmpParams = MutableLiveData<RPMParams>(RPMParams())
    override val rmpMediaUrl = MutableLiveData<String>()
    override val rmpState = MutableLiveData<PlaybackState>(PlaybackState.IDLE)

    // RPC Controller
    override var language: String = Locale.getDefault().language
    override val queryServiceId: String?
        get() = selectedService.value?.globalId
    override val mediaUrl: String?
        get() = rmpMediaUrl.value
    override val playbackState: PlaybackState
        get() = rmpState.value ?: PlaybackState.IDLE

    // User Agent Controller
    override val sltServices = MutableLiveData<List<SLSService>>()
    override val appData = MutableLiveData<AppData?>()

    init {
        atsc3Module.setListener(this)

        // TEST DATA
        selectedService.value = SLSService(5003, "WZTV", "tag:sinclairplatform.com,2020:WZTV:2727") //TODO: remove after tests
    }

    override fun onStateChanged(state: Atsc3Module.State?) {
        if (state == Atsc3Module.State.IDLE) {
            reset()
        }

        receiverState.postValue(state)
    }

    override fun onServicesLoaded(services: List<Service?>) {
        val slsServices = services.filterNotNull()
                .map { SLSService(it.serviceId, it.shortServiceName, it.globalServiceId) }
        sltServices.postValue(slsServices)
    }

    override fun onCurrentServiceHeldChanged(appContextId: String?, entryPage: String?) {
        appData.postValue(AppData(appContextId, entryPage))
    }

    override fun updateRMPPosition(scaleFactor: Double?, xPos: Double?, yPos: Double?) {
        rmpParams.postValue(RPMParams(
                scaleFactor ?: 100.0,
                xPos?.toInt() ?: 0,
                yPos?.toInt() ?: 0
        ))
    }

    override fun updateRMPState(state: PlaybackState) {
        when (state) {
            PlaybackState.PAUSED -> rmpPause()
            PlaybackState.PLAYING -> rmpResume()
            else -> {}
        }
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
        rmpReset()

        val res = atsc3Module.selectService(service.id)
        if (res) {
            //TODO: should we use globalId or context from HELD?
            selectedService.postValue(service)

            //TODO: temporary waiter for media. Should use notification instead
            GlobalScope.launch {
                var done = false
                var iterator = 5
                while (!done && iterator > 0) {
                    iterator--
                    delay(200)

                    done = withContext(Dispatchers.Main) {
                        val media = atsc3Module.getSelectedServiceMediaUri()
                        if (media != null) {
                            rmpMediaUrl.value = media
                        }

                        return@withContext (media != null)
                    }
                }
            }

            appData.value = AppData(
                    atsc3Module.getSelectedServiceAppContextId(),
                    atsc3Module.getSelectedServiceEntryPoint()
            )
        } else {
            appData.value = null
        }
    }

    override fun rmpReset() {
        rmpParams.postValue(RPMParams())
    }

    override fun rmpPlaybackChanged(state: PlaybackState) {
        rmpState.postValue(state)
    }

    override fun rmpPause() {
        rmpListeners.notifyPause(this)
    }

    override fun rmpResume() {
        rmpListeners.notifyResume(this)
    }

    override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
        rmpListeners.add(callback)
    }

    override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
        rmpListeners.remove(callback)
    }

    private fun reset() {
        selectedService.postValue(null)
        sltServices.postValue(emptyList())
        appData.postValue(null)
        rmpMediaUrl.postValue(null)
        rmpReset()
    }
}
