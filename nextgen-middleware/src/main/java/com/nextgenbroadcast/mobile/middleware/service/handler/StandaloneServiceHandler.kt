package com.nextgenbroadcast.mobile.middleware.service.handler

import android.os.*
import androidx.core.os.bundleOf
import com.nextgenbroadcast.mobile.core.getParcelable
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.model.ApplicationState
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.core.presentation.IReceiverPresenter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

internal class StandaloneServiceHandler(
        private val receiverPresenter: IReceiverPresenter,
        private val serviceController: IServiceController?,
        private val requireViewController: () -> IViewController
) : Handler() {

    private val stateScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    fun unSubscribeAll() {
        stateScope.cancel()
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        val playerStateListener: IObservablePlayer.IPlayerStateListener = PlayerStateListener(msg.replyTo)

        when (msg.what) {
            IServiceBinder.TYPE_ALL -> {
                observeReceiverState(msg.replyTo)
                observeSelectedService(msg.replyTo)
                observeServiceList(msg.replyTo)
                observeAppData(msg.replyTo)
                observeRPMLayoutParams(msg.replyTo)
                observeRPMMediaUrl(msg.replyTo)
                observeFrequency(msg.replyTo)
            }

            IServiceBinder.ACTION_OPEN_ROUTE -> {
                msg.data.getString(IServiceBinder.PARAM_OPEN_ROUTE_PATH)?.let { path ->
                    receiverPresenter.openRoute(path)
                }
            }

            IServiceBinder.ACTION_CLOSE_ROUTE -> receiverPresenter.closeRoute()

            IServiceBinder.ACTION_SELECT_SERVICE -> {
                msg.data.getParcelable(AVService::class.java, IServiceBinder.PARAM_SELECT_SERVICE)?.let {
//                    runBlocking {
//                        serviceController.selectService(it)
//                    }
                }
            }

            IServiceBinder.ACTION_RMP_LAYOUT_RESET -> {
//                requireViewController().rmpLayoutReset()
            }

            IServiceBinder.ACTION_RMP_PLAYBACK_STATE_CHANGED -> {
                msg.data.getParcelable(PlaybackState::class.java, IServiceBinder.PARAM_RMP_PLAYBACK_STATE)?.let {
                    requireViewController().rmpPlaybackChanged(it)
                }
            }

            IServiceBinder.ACTION_RMP_PLAYBACK_RATE_CHANGED -> {
                val speed = msg.data.getFloat(IServiceBinder.PARAM_RMP_PLAYBACK_RATE)
                requireViewController().rmpPlaybackRateChanged(speed)
            }

            IServiceBinder.ACTION_RMP_MEDIA_TIME_CHANGED -> {
                val currentTime = msg.data.getLong(IServiceBinder.PARAM_RMP_MEDIA_TIME)
                requireViewController().rmpMediaTimeChanged(currentTime)
            }

            IServiceBinder.CALLBACK_ADD_PLAYER_STATE_CHANGE -> {
                //requireViewController().addOnPlayerSateChangedCallback(playerStateListener)
            }

            IServiceBinder.CALLBACK_REMOVE_PLAYER_STATE_CHANGE -> {
                //requireViewController().addOnPlayerSateChangedCallback(playerStateListener)
            }

            IServiceBinder.ACTION_TYNE_FREQUENCY -> {
                msg.data.getParcelable(PhyFrequency::class.java, IServiceBinder.PARAM_FREQUENCY)?.let {
                    receiverPresenter.tune(it)
                }
            }

            IServiceBinder.ACTION_BA_STATE_CHANGED -> {
                msg.data.getParcelable(ApplicationState::class.java, IServiceBinder.PARAM_APPSTATE)?.let {
                    requireViewController().setApplicationState(it)
                }
            }

            else -> super.handleMessage(msg)
        }
    }

    inner class PlayerStateListener(
            private val sendToMessenger: Messenger
    ) : IObservablePlayer.IPlayerStateListener {

        override fun onStop(mediaController: IMediaPlayerPresenter?) {
            TODO("Not yet implemented")
        }

        override fun onPause(mediaController: IMediaPlayerPresenter?) {
            sendToMessenger.send(buildMessage(IServiceBinder.ACTION_PLAYER_STATE_CHANGE_PAUSE))
        }

        override fun onResume(mediaController: IMediaPlayerPresenter?) {
            sendToMessenger.send(buildMessage(IServiceBinder.ACTION_PLAYER_STATE_CHANGE_RESUME))
        }
    }

    private fun observeReceiverState(sendToMessenger: Messenger) {
//        stateScope.launch {
//            serviceController.receiverState.collect { state ->
//                sendToMessenger.send(buildMessage(
//                        IServiceBinder.TYPE_RECEIVER_STATE,
//                        bundleOf(
//                                IServiceBinder.PARAM_RECEIVER_STATE to state
//                        ),
//                        ReceiverState::class.java.classLoader
//                ))
//            }
//        }
    }

    private fun observeServiceList(sendToMessenger: Messenger) {
//        stateScope.launch {
//            serviceController.routeServices.collect { services ->
//                sendToMessenger.send(buildMessage(
//                        IServiceBinder.TYPE_SERVICE_LIST,
//                        bundleOf(
//                                IServiceBinder.PARAM_SERVICE_LIST to ArrayList<AVService>(services)
//                        ),
//                        AVService::class.java.classLoader
//                ))
//            }
//        }
    }

    private fun observeSelectedService(sendToMessenger: Messenger) {
//        stateScope.launch {
//            serviceController.selectedService.collect { selectedService ->
//                sendToMessenger.send(buildMessage(
//                        IServiceBinder.TYPE_SERVICE_SELECTED,
//                        bundleOf(
//                                IServiceBinder.PARAM_SERVICE_SELECTED to selectedService
//                        ),
//                        AVService::class.java.classLoader
//                ))
//            }
//        }
    }

    private fun observeAppData(sendToMessenger: Messenger) {
//        stateScope.launch {
//            requireViewController().appData.collect { appData ->
//                sendToMessenger.send(buildMessage(
//                        IServiceBinder.TYPE_APPDATA,
//                        bundleOf(
//                                IServiceBinder.PARAM_APPDATA to appData
//                        ),
//                        AppData::class.java.classLoader
//                ))
//            }
//        }
    }

    private fun observeRPMLayoutParams(sendToMessenger: Messenger) {
//        stateScope.launch {
//            requireViewController().rmpLayoutParams.collect { rpmLayoutParams ->
//                sendToMessenger.send(buildMessage(
//                        IServiceBinder.TYPE_RMP_LAYOUT_PARAMS,
//                        bundleOf(
//                                IServiceBinder.PARAM_RMP_LAYOUT_PARAMS to rpmLayoutParams
//                        ),
//                        RPMParams::class.java.classLoader
//                ))
//            }
//        }
    }

    private fun observeRPMMediaUrl(sendToMessenger: Messenger) {
//        stateScope.launch {
//            requireViewController().rmpMediaUri.collect { uri ->
//                sendToMessenger.send(buildMessage(
//                        IServiceBinder.TYPE_RMP_MEDIA_URI,
//                        bundleOf(
//                                IServiceBinder.PARAM_RMP_MEDIA_URI to uri
//                        )
//                ))
//            }
//        }
    }

    private fun observeFrequency(sendToMessenger: Messenger) {
//        stateScope.launch {
//            serviceController.receiverFrequency.collect { freqKhz ->
//                sendToMessenger.send(buildMessage(
//                        IServiceBinder.ACTION_TYNE_FREQUENCY,
//                        bundleOf(
//                                IServiceBinder.PARAM_FREQUENCY_KHZ to freqKhz
//                        ),
//                        AVService::class.java.classLoader
//                ))
//            }
//        }
    }

    private fun buildMessage(dataType: Int, args: Bundle? = null, classLoader: ClassLoader? = null): Message = Message.obtain(null, dataType).apply {
        args?.let {
            data = args
        }
        classLoader?.let {
            data.classLoader = classLoader
        }
    }
}