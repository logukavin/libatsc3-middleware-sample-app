package org.ngbp.jsonrpc4jtestharness.http.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.ngbp.jsonrpc4jtestharness.MainActivity
import org.ngbp.jsonrpc4jtestharness.R

class NotificationHelper {
    private var context: Context
    private var CHANNEL_ID: String = "ForegroundRpcServiceChannel"

    constructor(context: Context) {
        this.context = context
        createNotificationChannel()
    }

    constructor(context: Context, channelID: String) {
        this.context = context
        CHANNEL_ID = channelID
        createNotificationChannel()
    }

    fun createNotification(contentText: String?): Notification? {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, 0)
        return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Foreground Rpc Service")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Rpc Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }
}