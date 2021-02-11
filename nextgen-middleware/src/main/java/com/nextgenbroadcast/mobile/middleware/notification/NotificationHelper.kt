package com.nextgenbroadcast.mobile.middleware.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import com.nextgenbroadcast.mobile.core.model.PlaybackState
import com.nextgenbroadcast.mobile.middleware.R
import com.nextgenbroadcast.mobile.middleware.ServiceDialogActivity

class NotificationHelper(
        private val context: Context,
        private val channelID: String
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun createMediaNotificationBuilder(title: String, text: String, state: PlaybackState): Notification.Builder {
        val dialogIntent = Intent(context, ServiceDialogActivity::class.java)
        val contentIntent = PendingIntent.getActivity(context, 0, dialogIntent, 0)
        val actionIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)

        val builder = Notification.Builder(context, channelID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setVisibility(Notification.VISIBILITY_PUBLIC)

        when (state) {
            PlaybackState.PLAYING -> {
                builder.addAction(createAction(context, android.R.drawable.ic_media_pause, R.string.notification_pause_btn_title, actionIntent))
                builder.style = Notification.MediaStyle().setShowActionsInCompactView(0)
            }

            PlaybackState.PAUSED -> {
                builder.addAction(createAction(context, android.R.drawable.ic_media_play, R.string.notification_play_btn_title, actionIntent))
                builder.style = Notification.MediaStyle().setShowActionsInCompactView(0)
            }

            else -> {
            }
        }

        return builder
    }

    private fun createAction(context: Context, iconResId: Int, titleResId: Int, pendingIntent: PendingIntent) =
            Notification.Action.Builder(
                    Icon.createWithResource(context, iconResId),
                    context.getString(titleResId),
                    pendingIntent
            ).build()

    fun createNotificationChannel(name: String) {
        notificationManager.createNotificationChannel(
                NotificationChannel(channelID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    importance = NotificationManager.IMPORTANCE_LOW
                }
        )
    }

    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }

    fun cancel(id: Int) {
        notificationManager.cancel(id)
    }

    fun createMediaNotificationBuilder(container: NotificationContainer): Notification {
        return createMediaNotificationBuilder(container.title, container.message, container.state).build()
    }
}
