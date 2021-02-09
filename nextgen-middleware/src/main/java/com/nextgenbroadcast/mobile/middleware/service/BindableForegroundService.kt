package com.nextgenbroadcast.mobile.middleware.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.core.model.AVService
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.notification.NotificationHelper

abstract class BindableForegroundService : LifecycleMediaBrowserService() {
    private lateinit var notificationHelper: NotificationHelper

    protected var isForeground = false
        private set
    protected var isBinded = false
        private set
    protected var isStartedAsForeground = false
        private set

    override fun onCreate() {
        super.onCreate()

        notificationHelper = NotificationHelper(this, NOTIFICATION_CHANNEL_ID).also {
            it.createNotificationChannel(getString(R.string.atsc3_chanel_name))
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        notificationHelper.cancel(NOTIFICATION_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isStartedAsForeground = isStartedAsForeground or (intent?.getBooleanExtra(EXTRA_FOREGROUND, false)
                ?: false)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        val binder = super.onBind(intent)

        startForeground()
        isBinded = true

        return binder
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
        isBinded = true
    }

    override fun onUnbind(intent: Intent): Boolean {
        super.onUnbind(intent)
        isBinded = false
        return true
    }

    protected fun startForeground() {
        if (isForeground) return
        isForeground = true

        startForeground(NOTIFICATION_ID, createNotificationBuilder(getReceiverState()).build())
    }

    protected fun stopForeground() {
        stopForeground(true)

        isForeground = false
    }

    protected fun createNotificationBuilder(state: ReceiverState? = null, service: AVService? = null, playbackState: PlaybackState? = null): Notification.Builder {
        val title = if (state == null || state == ReceiverState.IDLE) {
            getString(R.string.atsc3_source_is_not_initialized)
        } else {
            service?.shortName ?: getString(R.string.atsc3_no_service_available)
        }

        return notificationHelper.createMediaNotificationBuilder(title, "", playbackState ?: PlaybackState.IDLE)
    }

    protected fun pushNotification(notification: Notification.Builder) {
        pushNotification(notification.build())
    }

    protected fun pushNotification(notification: Notification) {
        notificationHelper.notify(NOTIFICATION_ID, notification)
    }

    protected abstract fun getReceiverState(): ReceiverState

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Atsc3ServiceChannel"
        private const val NOTIFICATION_ID = 1

        const val EXTRA_FOREGROUND = "foreground"
    }
}