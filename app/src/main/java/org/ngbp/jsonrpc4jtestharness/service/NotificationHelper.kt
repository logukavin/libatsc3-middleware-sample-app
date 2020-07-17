package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.ngbp.jsonrpc4jtestharness.BuildConfig
import org.ngbp.jsonrpc4jtestharness.MainActivity
import org.ngbp.jsonrpc4jtestharness.R

class NotificationHelper {
    private var context: Context
    private var CHANNEL_ID: String = "ForegroundRpcServiceChannel"


    companion object {
        const val PLAYER_ACTION_PLAY = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PLAY"
        const val PLAYER_ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.intent.action.RMP_PAUSE"
    }

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
                .addAction(R.mipmap.ic_play, context.getString(R.string.notification_play_btn_title), getPendingIntent(PLAYER_ACTION_PLAY)) // #0
                .addAction(R.mipmap.ic_pause, context.getString(R.string.notification_pause_btn_title), getPendingIntent(PLAYER_ACTION_PAUSE)) // #1
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
                .setContentIntent(pendingIntent)
                .build()
    }

    private fun getPendingIntent(intentAction: String): PendingIntent {
        return PendingIntent.getActivity(context, 0, getPlayerIntent(intentAction), 0)
    }

    private fun getPlayerIntent(payerAction: String): Intent {
        return Intent().apply {
            action = payerAction
        }

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
