package com.nextgenbroadcast.mobile.middleware.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class AlertNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlertNotificationHelper.ACTION_DISMISS) {
            intent.getStringExtra(AlertNotificationHelper.ALERT_NOTIFICATION_TAG)?.let { tag ->
                NotificationManagerCompat.from(context).cancel(tag, tag.hashCode())
            }
        }
    }
}