package com.nextgenbroadcast.mobile.middleware.service.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import com.nextgenbroadcast.mobile.core.model.*
import com.nextgenbroadcast.mobile.middleware.controller.media.IObservablePlayer
import com.nextgenbroadcast.mobile.middleware.controller.service.IServiceController
import com.nextgenbroadcast.mobile.middleware.controller.view.IViewController
import com.nextgenbroadcast.mobile.middleware.presentation.IMediaPlayerPresenter
import com.nextgenbroadcast.mobile.middleware.presentation.IReceiverPresenter
import com.nextgenbroadcast.mobile.middleware.service.binder.InterprocessServiceBinder
import java.io.File

class Atsc3ServiceIncomingHandler(
        private val context: Context,
        private val lifecycleOwner: LifecycleOwner,
        private val receiverPresenter: IReceiverPresenter,
        private val serviceController: IServiceController,
        private val viewController: IViewController
) : Handler() {

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        val playerStateListener: IObservablePlayer.IPlayerStateListener = PlayerStateListener(msg.replyTo)

        when (msg.what) {

            InterprocessServiceBinder.LIVEDATA_ALL -> {
                observReceiverState(msg.replyTo)
                observServiceState(msg.replyTo)
                observeSelectedService(msg.replyTo)
                observeAppData(msg.replyTo)
                observeRPMLayoutParams(msg.replyTo)
                observeRPMMediaUrl(msg.replyTo)
            }

            InterprocessServiceBinder.ACTION_OPEN_ROUTE -> {
                msg.data.getString(InterprocessServiceBinder.PARAM_OPEN_ROUTE_PATH)?.let { path ->
                    receiverPresenter.openRoute(path)
                }
            }

            InterprocessServiceBinder.ACTION_CLOSE_ROUTE -> receiverPresenter.closeRoute()

            InterprocessServiceBinder.ACTION_SELECT_SERVICE -> {
                msg.data.getParcelable(SLSService::class.java, InterprocessServiceBinder.PARAM_SELECT_SERVICE)?.let {
                    serviceController.selectService(it)
                }
            }

            InterprocessServiceBinder.ACTION_RMP_LAYOUT_RESET -> viewController.rmpLayoutReset()

            InterprocessServiceBinder.ACTION_RMP_PLAYBACK_STATE_CHANGED -> {
                msg.data.getParcelable(PlaybackState::class.java, InterprocessServiceBinder.PARAM_RMP_PLAYBACK_STATE)?.let {
                    viewController.rmpPlaybackChanged(it)
                }
            }

            InterprocessServiceBinder.ACTION_RMP_PLAYBACK_RATE_CHANGED -> {
                val speed = msg.data.getFloat(InterprocessServiceBinder.PARAM_RMP_PLAYBACK_RATE)
                viewController.rmpPlaybackRateChanged(speed)
            }

            InterprocessServiceBinder.ACTION_RMP_MEDIA_TIME_CHANGED -> {
                val currentTime = msg.data.getLong(InterprocessServiceBinder.PARAM_RMP_MEDIA_TIME)
                viewController.rmpMediaTimeChanged(currentTime)
            }

            InterprocessServiceBinder.CALLBACK_ADD_PLAYER_STATE_CHANGE -> {
                viewController.addOnPlayerSateChangedCallback(playerStateListener)
            }

            InterprocessServiceBinder.CALLBACK_REMOVE_PLAYER_STATE_CHANGE -> {
                viewController.addOnPlayerSateChangedCallback(playerStateListener)
            }

            else -> super.handleMessage(msg)
        }
    }

    inner class PlayerStateListener(
            private val sendToMessenger: Messenger
    ) : IObservablePlayer.IPlayerStateListener {

        override fun onPause(mediaController: IMediaPlayerPresenter) {
            sendToMessenger.send(buildMessage(InterprocessServiceBinder.ACTION_PLAYER_STATE_CHANGE_PAUSE))
        }

        override fun onResume(mediaController: IMediaPlayerPresenter) {
            sendToMessenger.send(buildMessage(InterprocessServiceBinder.ACTION_PLAYER_STATE_CHANGE_RESUME))
        }
    }

    private fun observReceiverState(sendToMessenger: Messenger) {
        serviceController.receiverState.observe(lifecycleOwner, { state ->
            sendToMessenger.send(buildMessage(
                    InterprocessServiceBinder.LIVEDATA_RECEIVER_STATE,
                    bundleOf(
                            InterprocessServiceBinder.PARAM_RECEIVER_STATE to state
                    ),
                    ReceiverState::class.java.classLoader
            ))
        })
    }

    private fun observServiceState(sendToMessenger: Messenger) {
        serviceController.sltServices.observe(lifecycleOwner, { services ->
            sendToMessenger.send(buildMessage(
                    InterprocessServiceBinder.LIVEDATA_SERVICE_LIST,
                    bundleOf(
                            InterprocessServiceBinder.PARAM_SERVICE_LIST to ArrayList<SLSService>(services)
                    ),
                    SLSService::class.java.classLoader
            ))
        })
    }

    private fun observeSelectedService(sendToMessenger: Messenger) {
        serviceController.selectedService.observe(lifecycleOwner, { selectedService ->
            sendToMessenger.send(buildMessage(
                    InterprocessServiceBinder.LIVEDATA_SERVICE_SELECTED,
                    bundleOf(
                            InterprocessServiceBinder.PARAM_SERVICE_SELECTED to selectedService
                    ),
                    SLSService::class.java.classLoader
            ))
        })
    }

    private fun observeAppData(sendToMessenger: Messenger) {
        viewController.appData.observe(lifecycleOwner, { appData ->
            sendToMessenger.send(buildMessage(
                    InterprocessServiceBinder.LIVEDATA_APPDATA,
                    bundleOf(
                            InterprocessServiceBinder.PARAM_APPDATA to appData
                    ),
                    AppData::class.java.classLoader
            ))
        })
    }

    private fun observeRPMLayoutParams(sendToMessenger: Messenger) {
        viewController.rmpLayoutParams.observe(lifecycleOwner, { rpmLayoutParams ->
            sendToMessenger.send(buildMessage(
                    InterprocessServiceBinder.LIVEDATA_RMP_LAYOUT_PARAMS,
                    bundleOf(
                            InterprocessServiceBinder.PARAM_RMP_LAYOUT_PARAMS to rpmLayoutParams
                    ),
                    RPMParams::class.java.classLoader
            ))
        })
    }

    private fun observeRPMMediaUrl(sendToMessenger: Messenger) {
        viewController.rmpMediaUrl.observe(lifecycleOwner, { rmpMediaUrl ->
            val contentUri: Uri? = rmpMediaUrl?.let {
                val file = File("file://$rmpMediaUrl")
                Log.d("TEST", "file exists: ${file.exists()}")
                Log.d("TEST", "Context.getCacheDir(${context.cacheDir})")
                val uri = getUriForFile(context, "com.nextgenbroadcast.mobile.middleware.provider", file)
                context.grantUriPermission("com.nextgenbroadcast.mobile.middleware.simple", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.grantUriPermission("com.nextgenbroadcast.mobile.middleware.simple.light", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.grantUriPermission("com.nextgenbroadcast.mobile.middleware.simple.standalone", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri
            }
            sendToMessenger.send(buildMessage(
                    InterprocessServiceBinder.LIVEDATA_RMP_MEDIA_URL,
                    bundleOf(
                            InterprocessServiceBinder.PARAM_RMP_MEDIA_URI to contentUri
                    )
            ))
        })
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