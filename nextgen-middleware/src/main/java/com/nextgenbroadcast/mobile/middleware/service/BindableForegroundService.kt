package com.nextgenbroadcast.mobile.middleware.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.SLSService
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.notification.NotificationHelper
import kotlinx.coroutines.*

abstract class BindableForegroundService : LifecycleService() {
    private lateinit var notificationHelper: NotificationHelper

    protected var isForeground = false
        private set
    protected var isBinded = false
        private set
    private var isStartedAsForeground = false
    private var destroyPresentationLayerJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        notificationHelper = NotificationHelper(this, NOTIFICATION_CHANNEL_ID).also {
            it.createNotificationChannel(getString(R.string.atsc3_chanel_name))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStartedAsForeground = isStartedAsForeground or (intent?.getBooleanExtra(EXTRA_FOREGROUND, false)
                ?: false)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)

        startForeground()

        cancelPresentationDestroying()

        createViewPresentationAndStartService()

        isBinded = true

        return null
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        cancelPresentationDestroying()

        isBinded = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        super.onUnbind(intent)

        isBinded = false

        destroyPresentationLayerJob?.cancel()
        if (isStartedAsForeground) {
            destroyPresentationLayerJob = CoroutineScope(Dispatchers.IO).launch {
                delay(PRESENTATION_DESTROYING_DELAY)
                withContext(Dispatchers.Main) {
                    destroyViewPresentationAndStopService()
                    destroyPresentationLayerJob = null
                }
            }
        } else {
            destroyViewPresentationAndStopService()
        }

        return true
    }

    protected fun startForeground() {
        if (isForeground) return
        isForeground = true

        startForeground(NOTIFICATION_ID, createNotification(getReceiverState()))
    }

    protected fun stopForeground() {
        stopForeground(true)

        isForeground = false
    }

    protected fun createNotification(state: ReceiverState? = null, service: SLSService? = null, playbackState: PlaybackState? = null): Notification {
        val title = if (state == null || state == ReceiverState.IDLE) {
            getString(R.string.atsc3_source_is_not_initialized)
        } else {
            service?.shortName ?: getString(R.string.atsc3_no_service_available)
        }

        return notificationHelper.createMediaNotification(title, "", playbackState
                ?: PlaybackState.IDLE)
    }

    protected fun pushNotification(notification: Notification) {
        notificationHelper.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelPresentationDestroying() {
        destroyPresentationLayerJob?.let {
            it.cancel()
            destroyPresentationLayerJob = null
        }
    }

    protected abstract fun getReceiverState(): ReceiverState
    protected abstract fun createViewPresentationAndStartService()
    protected abstract fun destroyViewPresentationAndStopService()

    companion object {
        private const val PRESENTATION_DESTROYING_DELAY = 1000L

        private const val NOTIFICATION_CHANNEL_ID = "Atsc3ServiceChannel"
        private const val NOTIFICATION_ID = 1

        const val EXTRA_FOREGROUND = "foreground"
    }
}