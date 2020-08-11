package com.nextgenbroadcast.mobile.middleware.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.core.model.ReceiverState
import com.nextgenbroadcast.mobile.middleware.Atsc3ForegroundService
import com.nextgenbroadcast.mobile.middleware.R

class NotificationHelper(
        private val context: Context,
        private val channelID: String
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun createMediaNotification(title: String, text: String, state: PlaybackState): Notification {
        val builder = createNotificationBuilder(title, text)

        when (state) {
            PlaybackState.PLAYING -> {
                builder.addAction(createAction(context, android.R.drawable.ic_media_pause, R.string.notification_pause_btn_title, Atsc3ForegroundService.ACTION_RMP_PAUSE))
                builder.style = Notification.MediaStyle().setShowActionsInCompactView(0)
            }

            PlaybackState.PAUSED -> {
                builder.addAction(createAction(context, android.R.drawable.ic_media_play, R.string.notification_play_btn_title, Atsc3ForegroundService.ACTION_RMP_PLAY))
                builder.style = Notification.MediaStyle().setShowActionsInCompactView(0)
            }

            else -> {
            }
        }

        return builder.build()
    }

    private fun createAction(context: Context, @DrawableRes iconResId: Int, @StringRes titleResId: Int, intentAction: String): Notification.Action {
        val pendingIntent = createPendingIntent(context, intentAction)
        return Notification.Action.Builder(
                Icon.createWithResource(context, iconResId),
                context.getString(titleResId),
                pendingIntent
        ).build()
    }

    private fun createPendingIntent(context: Context, intentAction: String? = null): PendingIntent {
        val intent = Intent(context, Atsc3ForegroundService::class.java).apply {
            action = intentAction
        }
        return PendingIntent.getService(context, 0, intent, 0)
    }

    fun createNotificationChannel(name: String) {
        notificationManager.createNotificationChannel(
                NotificationChannel(channelID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    importance = NotificationManager.IMPORTANCE_HIGH
                }
        )
    }

    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }

    fun createMediaNotification(container: NotificationContainer): Notification {
        return createMediaNotification(container.title, container.message, container.state)
    }

    fun createAtsc3SourceStateNotification(sourceState: ReceiverState): Notification {
        val title = if (sourceState == ReceiverState.OPENED)
            context.getString(R.string.atsc3_source_is_initialized)
        else
            context.getString(R.string.atsc3_source_is_not_initialized)

        return createNotificationBuilder(title).build()
    }

    private fun createNotificationBuilder(title: String, text: String = ""): Notification.Builder {
        val pendingIntent = createPendingIntent(context)
        return Notification.Builder(context, channelID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(text)
    }
}
