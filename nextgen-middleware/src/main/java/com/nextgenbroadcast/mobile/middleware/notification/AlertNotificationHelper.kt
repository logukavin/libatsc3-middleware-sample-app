package com.nextgenbroadcast.mobile.middleware.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nextgenbroadcast.mobile.middleware.R

class AlertNotificationHelper(private val context: Context) {
    private val att = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

    private val notificationChannel =
        NotificationChannel(ALERT_CHANNEL_ID, ALERT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).also {
            it.setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.beep_attention_signal_440_hz_3),
                att
            )
            it.importance = NotificationManager.IMPORTANCE_HIGH
            it.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

     fun showNotification(msg: String, messageId:Int) {


        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_warning_24)
            .setContentTitle(context.resources.getString(R.string.warning))
            .setContentText(msg)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        val notification = builder.build()

        with(NotificationManagerCompat.from(context)) {
            notify(messageId, notification)
        }
    }

    companion object {
        const val ALERT_CHANNEL_ID = "2345"
        const val ALERT_CHANNEL_NAME = "alert_channel"
    }
}