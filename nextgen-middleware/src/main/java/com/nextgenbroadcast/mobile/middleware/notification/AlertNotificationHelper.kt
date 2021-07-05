package com.nextgenbroadcast.mobile.middleware.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.nextgenbroadcast.mobile.middleware.AlertDialogActivity
import com.nextgenbroadcast.mobile.middleware.R

class AlertNotificationHelper(
    private val context: Context
) {

    private val packageName = context.packageName
    private val notificationChannel =
        NotificationChannel(
            ALERT_CHANNEL_ID,
            ALERT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + R.raw.beep_attention_signal_440_hz_3),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
            importance = NotificationManager.IMPORTANCE_HIGH
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notificationManager.createNotificationChannel(notificationChannel)
    }

    fun showNotification(msg: String, msgTag: String, msgTime: String?) {
        val aeaId = msgTag.hashCode()

        val intent = Intent(context, AlertNotificationReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(ALERT_NOTIFICATION_TAG, msgTag)
        }

        val contentIntent = AlertDialogActivity.newIntent(context, msgTag, msg, msgTime)

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            aeaId,
            contentIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val pendingDismissIntent = PendingIntent.getBroadcast(
            context,
            aeaId,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val contentView = RemoteViews(packageName, R.layout.alert_notification_view)
        val contentViewExpanded = RemoteViews(packageName, R.layout.alert_notification_expanded_view)

        contentView.setTextViewText(R.id.textViewAlertTitle, context.getString(R.string.warning))
        contentView.setTextViewText(R.id.textViewAlertText, msg)

        contentViewExpanded.setTextViewText(
            R.id.textViewNotificationExpandedTitle,
            context.getString(R.string.warning)
        )
        contentViewExpanded.setTextViewText(R.id.textViewAlertExpandedText, msg)
        contentViewExpanded.setOnClickPendingIntent(
            R.id.textViewAlertExpandedDismiss,
            pendingDismissIntent
        )

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setCustomContentView(contentView)
            .setCustomBigContentView(contentViewExpanded)
            .setContentIntent(pendingContentIntent)
            .setGroup(ALERT_GROUP_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        val notification = builder.build().apply {
            flags = Notification.FLAG_NO_CLEAR
        }
        notificationManager.notify(msgTag, aeaId, notification)
    }

    companion object {
        const val ALERT_CHANNEL_ID = "2345"
        const val ALERT_GROUP_ID = "4765"
        const val ALERT_CHANNEL_NAME = "alert_channel"
        const val ALERT_NOTIFICATION_TAG = "notificationTag"
        const val ACTION_DISMISS = "DISMISS"
    }
}