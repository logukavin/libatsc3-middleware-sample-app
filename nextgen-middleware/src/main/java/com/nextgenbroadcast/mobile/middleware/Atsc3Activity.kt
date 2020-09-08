package com.nextgenbroadcast.mobile.middleware

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.middleware.controller.media.IObservablePlayer
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.ISelectorPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IUserAgentPresenter

abstract class Atsc3Activity : AppCompatActivity() {
    var isBound: Boolean = false
        private set

    private val mMessenger: Messenger = Messenger(IncomingHandler())
    private var atsc3Service: Messenger? = null

    override fun onStart() {
        super.onStart()

        Intent(this, Atsc3ForegroundService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()

        unbindService(connection)
        isBound = false
        onUnbind()
    }

    abstract fun onBind(binder: IServiceBinder)
    abstract fun onUnbind()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            //val binder = service as? Atsc3ForegroundService.ServiceBinder ?: return
            //onBind(binder)

            //********************/
            atsc3Service = Messenger(service)
            isBound = true
            subscribeReceiverState()
            subscribeSltServices()
            subscribeRPMLayoutParams()
            subscribeSelectedService()
            subscribeAppData()

            subscribeRPMMediaUrl()
            onBind(handlerBinder)
            //********************/
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            atsc3Service = null
            isBound = false
        }
    }

        inner class IncomingHandler : Handler() {

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d("TEST", "from service msg: ${msg.what}")
            when(msg.what) {
                BindableForegroundService.RECEIVER_STATE -> try {
                    msg.data.classLoader = ReceiverState::class.java.classLoader
                    val receiverState = msg.data.getParcelable<ReceiverState>("RECEIVER_STATE")
                    Log.d("TEST", "Receive state: ${receiverState?.name}")
                    receiverState?.let {
                        receiverPresenter.receiverState.postValue(receiverState)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                BindableForegroundService.SLT_SERVICE_STATE -> try {
                    msg.data.classLoader = SLSService::class.java.classLoader
                    val sltServices = msg.data.getParcelableArrayList<SLSService>("SLT_SERVICE_STATE")
                    Log.d("TEST", "Receive services: ${sltServices?.size}")
                    sltServices?.let { it ->
                        selectorPresenter.sltServices.postValue(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                BindableForegroundService.SELECTED_SERVICE_STATE -> try {
                    msg.data.classLoader = SLSService::class.java.classLoader
                    val selectedService = msg.data.getParcelable<SLSService?>("SELECTED_SERVICE_STATE")
                    Log.d("TEST", "Receive selected: $selectedService")
                    selectorPresenter.selectedService.postValue(selectedService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                BindableForegroundService.APP_DATA_SERVICE_STATE -> try {
                    val appData = msg.data.getParcelable<AppData?>("app_data")
                    Log.d("TEST", "Receive appData: $appData")
                    userAgentPresenter.appData.postValue(appData)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                BindableForegroundService.RMP_LAYOUT_PARAMS -> try {
                    msg.data.classLoader = RPMParams::class.java.classLoader
                    val rpmParams = msg.data.getParcelable<RPMParams>("rpm_layout_params")
                    Log.d("TEST", "Receive rpm params: $rpmParams")
                    mediaPlayerPresenter.rmpLayoutParams.postValue(rpmParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                BindableForegroundService.RMP_MEDIA_URL -> try {
                    msg.data.classLoader = String::class.java.classLoader
                    val rpmMediaUrl = msg.data.getString("rpm_media_url")
                    Log.d("TEST", "Receive rpm media url: $rpmMediaUrl")
                    mediaPlayerPresenter.rmpMediaUrl.postValue(rpmMediaUrl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val selectorPresenter = object : ISelectorPresenter {
        override val sltServices = MutableLiveData<List<SLSService>>()
        override val selectedService = MutableLiveData<SLSService?>()

        override fun selectService(service: SLSService) {
            sendSelectService(service)
        }
    }

    private val receiverPresenter = object : IReceiverPresenter {
        override val receiverState = MutableLiveData<ReceiverState>()

        override fun openRoute(path: String): Boolean {
            sendOpenRoute(path)
            return true
        }

        override fun closeRoute() {
            sendCloseRoute()
        }
    }

    private val userAgentPresenter = object : IUserAgentPresenter{
        override val appData = MutableLiveData<AppData?>()
    }

    private val mediaPlayerPresenter = object : IMediaPlayerPresenter {
        override val rmpLayoutParams = MutableLiveData<RPMParams>()
        override val rmpMediaUrl = MutableLiveData<String?>()

        override fun rmpLayoutReset() {
            sendRPMLayoutReset()
        }

        override fun rmpPlaybackChanged(state: PlaybackState) {
            sendRPMPlaybackChecked(state)
        }

        override fun rmpPlaybackRateChanged(speed: Float) {
            sendRPMPlaybackRateChanged(speed)
        }

        override fun rmpMediaTimeChanged(currentTime: Long) {
            sendRPMMediaTimeChanged(currentTime)
        }

        override fun addOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
            //TODO("Not yet implemented")
        }

        override fun removeOnPlayerSateChangedCallback(callback: IObservablePlayer.IPlayerStateListener) {
            //TODO("Not yet implemented")
        }
    }

    val handlerBinder = object: IServiceBinder {
        override fun getReceiverPresenter(): IReceiverPresenter = receiverPresenter

        override fun getSelectorPresenter(): ISelectorPresenter = selectorPresenter

        override fun getUserAgentPresenter(): IUserAgentPresenter = userAgentPresenter

        override fun getMediaPlayerPresenter(): IMediaPlayerPresenter = mediaPlayerPresenter
    }

    private fun subscribeReceiverState() {
        Log.d("TEST", "subscribeReceiverState()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.RECEIVER_STATE, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendOpenRoute(path: String) {
        Log.d("TEST", "sendOpenRoute($path)")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.OPEN_ROUTE_STATE, 0, 0)
        msg.data.putString("OPEN_ROUTE_STATE", path)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendCloseRoute() {
        Log.d("TEST", "sendCloseRoute()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.CLOSE_ROUTE_STATE, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun subscribeSltServices() {
        Log.d("TEST", "subscribeSltServices()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.SLT_SERVICE_STATE, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun subscribeSelectedService() {
        Log.d("TEST", "subscribeSelectedService()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.SELECTED_SERVICE_STATE, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendSelectService(service: SLSService) {
        Log.d("TEST", "selectService($service)")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.SELECT_SERVICE_STATE, 0, 0)
        msg.data.classLoader = SLSService::class.java.classLoader
        msg.data.putParcelable("SELECT_SERVICE_STATE", service)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun subscribeAppData() {
        Log.d("TEST", "subscribeAppData()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.APP_DATA_SERVICE_STATE, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun subscribeRPMLayoutParams() {
        Log.d("TEST", "subscribeRPMLayoutParams()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.RMP_LAYOUT_PARAMS, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun subscribeRPMMediaUrl() {
        Log.d("TEST", "subscribeRPMMediaUrl()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.RMP_MEDIA_URL, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendRPMLayoutReset() {
        Log.d("TEST", "sendRPMLayoutReset()")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.RMP_LAYOUT_RESET, 0, 0)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendRPMPlaybackChecked(state: PlaybackState) {
        Log.d("TEST", "sendRPMPlaybackChecked($state)")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.RMP_PLAYBACK_CHECKED, 0, 0)
        msg.data.classLoader = PlaybackState::class.java.classLoader
        msg.data.putParcelable("RMP_PLAYBACK_CHECKED", state)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendRPMPlaybackRateChanged(speed: Float) {
        Log.d("TEST", "sendRPMPlaybackRateChanged($speed)")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.RMP_PLAYBACK_RATE_CHANGED, 0, 0)
        msg.data.putFloat("RMP_PLAYBACK_RATE_CHANGED", speed)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun sendRPMMediaTimeChanged(currentTime: Long) {
        Log.d("TEST", "sendRPMMediaTimeChanged($currentTime)")
        if (!isBound) return
        val msg: Message = Message.obtain(null, BindableForegroundService.RMP_MEDIA_TIME_CHANGED, 0, 0)
        msg.data.putLong("RMP_PLAYBACK_RATE_CHANGED", currentTime)
        msg.replyTo = mMessenger
        try {
            atsc3Service?.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}