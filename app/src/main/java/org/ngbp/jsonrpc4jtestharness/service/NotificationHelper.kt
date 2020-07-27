package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.ngbp.jsonrpc4jtestharness.NotificationReceiverActivity
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.core.model.PlaybackState

class NotificationHelper(
        private val context: Context,
        private val channelID: String
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun createMediaNotification(title: String, text: String, state: PlaybackState): Notification {
        val notificationIntent = Intent(context, NotificationReceiverActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)

        val builder = Notification.Builder(context, channelID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)

        when (state) {
            PlaybackState.PLAYING -> {
                builder.addAction(createAction(context, android.R.drawable.ic_media_play, R.string.notification_play_btn_title, NotificationReceiverActivity.PLAYER_ACTION_PLAY))
                builder.style = Notification.MediaStyle().setShowActionsInCompactView(0)
            }
            PlaybackState.PAUSED -> {
                builder.addAction(createAction(context, android.R.drawable.ic_media_pause, R.string.notification_pause_btn_title, NotificationReceiverActivity.PLAYER_ACTION_PAUSE))
                builder.style = Notification.MediaStyle().setShowActionsInCompactView(0)
            }
            else -> {
            }
        }

        return builder.build()
    }

    private fun createAction(context: Context, @DrawableRes iconResId: Int, @StringRes titleResId: Int, intentAction: String): Notification.Action {
        val intent = Intent(context, NotificationReceiverActivity::class.java).apply {
            action = intentAction
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        return Notification.Action.Builder(
                Icon.createWithResource(context, iconResId),
                context.getString(titleResId),
                pendingIntent).build()
    }

    fun createNotificationChannel(name: String) {
        notificationManager.createNotificationChannel(NotificationChannel(channelID, name, NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun notify(id: Int, notification: Notification) {
        notificationManager.notify(id, notification)
    }

    fun createMediaNotification(container: NotificationContainer): Notification {
        return createMediaNotification(container.title, container.message, container.state)
    }
}
