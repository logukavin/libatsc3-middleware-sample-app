package com.nextgenbroadcast.mobile.middleware.service.handler

import android.net.Uri
import android.os.*
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import com.nextgenbroadcast.mobile.core.getParcelable
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.core.presentation.media.IObservablePlayer
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.core.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.core.service.binder.IServiceBinder
import com.nextgenbroadcast.mobile.middleware.IMediaFileProvider
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter

internal class StandaloneServiceHandler(
        private val fileProvider: IMediaFileProvider,
        private val lifecycleOwner: LifecycleOwner,
        private val receiverPresenter: IReceiverPresenter,
        private val serviceController: IServiceController,
        private val viewController: IViewController
) : Handler() {

    fun unSubscribeAll() {
        serviceController.receiverState.removeObservers(lifecycleOwner)
        serviceController.sltServices.removeObservers(lifecycleOwner)
        serviceController.selectedService.removeObservers(lifecycleOwner)
        viewController.appData.removeObservers(lifecycleOwner)
        viewController.rmpLayoutParams.removeObservers(lifecycleOwner)
        viewController.rmpMediaUri.removeObservers(lifecycleOwner)
        fileProvider.revokeAllUriPermissions()
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        val playerStateListener: IObservablePlayer.IPlayerStateListener = PlayerStateListener(msg.replyTo)

        when (msg.what) {

            IServiceBinder.LIVEDATA_ALL -> {
                observeReceiverState(msg.replyTo)
                observeSelectedService(msg.replyTo)
                observeServiceList(msg.replyTo)
                observeAppData(msg.replyTo)
                observeRPMLayoutParams(msg.replyTo)
                msg.data.getString(IServiceBinder.PARAM_PERMISSION_PACKAGE)?.let { clientPackage ->
                    observeRPMMediaUrl(msg.replyTo, clientPackage)
                }
            }

            IServiceBinder.ACTION_OPEN_ROUTE -> {
                msg.data.getString(IServiceBinder.PARAM_OPEN_ROUTE_PATH)?.let { path ->
                    receiverPresenter.openRoute(path)
                }
            }

            IServiceBinder.ACTION_CLOSE_ROUTE -> receiverPresenter.closeRoute()

            IServiceBinder.ACTION_SELECT_SERVICE -> {
                msg.data.getParcelable(SLSService::class.java, IServiceBinder.PARAM_SELECT_SERVICE)?.let {
                    serviceController.selectService(it)
                }
            }

            IServiceBinder.ACTION_RMP_LAYOUT_RESET -> viewController.rmpLayoutReset()

            IServiceBinder.ACTION_RMP_PLAYBACK_STATE_CHANGED -> {
                msg.data.getParcelable(PlaybackState::class.java, IServiceBinder.PARAM_RMP_PLAYBACK_STATE)?.let {
                    viewController.rmpPlaybackChanged(it)
                }
            }

            IServiceBinder.ACTION_RMP_PLAYBACK_RATE_CHANGED -> {
                val speed = msg.data.getFloat(IServiceBinder.PARAM_RMP_PLAYBACK_RATE)
                viewController.rmpPlaybackRateChanged(speed)
            }

            IServiceBinder.ACTION_RMP_MEDIA_TIME_CHANGED -> {
                val currentTime = msg.data.getLong(IServiceBinder.PARAM_RMP_MEDIA_TIME)
                viewController.rmpMediaTimeChanged(currentTime)
            }

            IServiceBinder.CALLBACK_ADD_PLAYER_STATE_CHANGE -> {
                viewController.addOnPlayerSateChangedCallback(playerStateListener)
            }

            IServiceBinder.CALLBACK_REMOVE_PLAYER_STATE_CHANGE -> {
                viewController.addOnPlayerSateChangedCallback(playerStateListener)
            }

            IServiceBinder.ACTION_NEED_URI_PERMISSION -> {
                msg.data.getParcelable(Uri::class.java, IServiceBinder.PARAM_URI_NEED_PERMISSION)?.let { uri ->
                    msg.data.getString(IServiceBinder.PARAM_PERMISSION_PACKAGE)?.let { clientPackage ->
                        fileProvider.grantUriPermission(clientPackage, uri)
                        uri.path?.let { uriPath ->
                            sendHavePermissions(msg.replyTo, uriPath)
                        }
                    }
                }
            }

            else -> super.handleMessage(msg)
        }
    }

    inner class PlayerStateListener(
            private val sendToMessenger: Messenger
    ) : IObservablePlayer.IPlayerStateListener {

        override fun onPause(mediaController: IMediaPlayerPresenter) {
            sendToMessenger.send(buildMessage(IServiceBinder.ACTION_PLAYER_STATE_CHANGE_PAUSE))
        }

        override fun onResume(mediaController: IMediaPlayerPresenter) {
            sendToMessenger.send(buildMessage(IServiceBinder.ACTION_PLAYER_STATE_CHANGE_RESUME))
        }
    }

    private fun observeReceiverState(sendToMessenger: Messenger) {
        serviceController.receiverState.observe(lifecycleOwner, { state ->
            sendToMessenger.send(buildMessage(
                    IServiceBinder.LIVEDATA_RECEIVER_STATE,
                    bundleOf(
                            IServiceBinder.PARAM_RECEIVER_STATE to state
                    ),
                    ReceiverState::class.java.classLoader
            ))
        })
    }

    private fun observeServiceList(sendToMessenger: Messenger) {
        serviceController.sltServices.observe(lifecycleOwner, { services ->
            sendToMessenger.send(buildMessage(
                    IServiceBinder.LIVEDATA_SERVICE_LIST,
                    bundleOf(
                            IServiceBinder.PARAM_SERVICE_LIST to ArrayList<SLSService>(services)
                    ),
                    SLSService::class.java.classLoader
            ))
        })
    }

    private fun observeSelectedService(sendToMessenger: Messenger) {
        serviceController.selectedService.observe(lifecycleOwner, { selectedService ->
            sendToMessenger.send(buildMessage(
                    IServiceBinder.LIVEDATA_SERVICE_SELECTED,
                    bundleOf(
                            IServiceBinder.PARAM_SERVICE_SELECTED to selectedService
                    ),
                    SLSService::class.java.classLoader
            ))
        })
    }

    private fun observeAppData(sendToMessenger: Messenger) {
        viewController.appData.observe(lifecycleOwner, { appData ->
            sendToMessenger.send(buildMessage(
                    IServiceBinder.LIVEDATA_APPDATA,
                    bundleOf(
                            IServiceBinder.PARAM_APPDATA to appData
                    ),
                    AppData::class.java.classLoader
            ))
        })
    }

    private fun observeRPMLayoutParams(sendToMessenger: Messenger) {
        viewController.rmpLayoutParams.observe(lifecycleOwner, { rpmLayoutParams ->
            sendToMessenger.send(buildMessage(
                    IServiceBinder.LIVEDATA_RMP_LAYOUT_PARAMS,
                    bundleOf(
                            IServiceBinder.PARAM_RMP_LAYOUT_PARAMS to rpmLayoutParams
                    ),
                    RPMParams::class.java.classLoader
            ))
        })
    }

    private fun observeRPMMediaUrl(sendToMessenger: Messenger, clientPackage: String) {
        viewController.rmpMediaUri.observe(lifecycleOwner, { rmpMediaUri ->
            rmpMediaUri?.let { uri ->
                fileProvider.grantUriPermission(clientPackage, uri)
                sendToMessenger.send(buildMessage(
                        IServiceBinder.LIVEDATA_RMP_MEDIA_URI,
                        bundleOf(
                                IServiceBinder.PARAM_RMP_MEDIA_URI to uri
                        )
                ))
            }
        })
    }

    private fun sendHavePermissions(sendToMessenger: Messenger, uriPath: String) {
        sendToMessenger.send(buildMessage(IServiceBinder.ACTION_NEED_URI_PERMISSION, bundleOf(
                IServiceBinder.PARAM_URI_NEED_PERMISSION to uriPath
        )))
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