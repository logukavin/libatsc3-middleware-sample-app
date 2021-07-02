package com.nextgenbroadcast.mobile.middleware.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class AlertNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationTag = intent.getStringExtra(AlertNotificationHelper.ALERT_NOTIFICATION_TAG)
        when(action){
            AlertNotificationHelper.ACTION_DISMISS ->  dismissNotification(context, notificationTag)
        }
    }

    private fun dismissNotification(context: Context, notificationTag:String?) {
        notificationTag?.let { tag ->
            NotificationManagerCompat.from(context).cancel(tag, tag.hashCode())
        }

    }
}