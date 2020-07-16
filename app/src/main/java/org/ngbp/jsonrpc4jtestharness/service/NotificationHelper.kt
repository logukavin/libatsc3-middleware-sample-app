package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.ngbp.jsonrpc4jtestharness.MainActivity
import org.ngbp.jsonrpc4jtestharness.PlayerBroadcastReceiver
import org.ngbp.jsonrpc4jtestharness.R

class NotificationHelper {
    private var context: Context
    private var CHANNEL_ID: String = "ForegroundRpcServiceChannel"
    private val PLAYER_ACTION_PLAY = "player_action_play"
    private val PLAYER_ACTION_STOP = "player_action_stop"
    private val PLAYER_ACTION = "player_action"

    private val PLAY = "Play"
    private val PAUSE = "Pause"

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
                .addAction(R.mipmap.ic_play, PLAY, getPlayIntent()) // #0
                .addAction(R.mipmap.ic_pause, PAUSE, getPauseIntent()) // #1
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
                .setContentIntent(pendingIntent)
                .build()
    }

    private fun getPauseIntent(): PendingIntent {
        return PendingIntent.getBroadcast(context, 0, getPlayerIntent(PLAYER_ACTION_PLAY, PAUSE), 0)
    }

    private fun getPlayIntent(): PendingIntent {
        return PendingIntent.getBroadcast(context, 0, getPlayerIntent(PLAYER_ACTION_STOP, PLAY), 0)
    }

    private fun getPlayerIntent(payerAction: String, btnAction: String): Intent {
        return Intent(context, PlayerBroadcastReceiver::class.java).apply {
            action = payerAction
            putExtra(PLAYER_ACTION, btnAction)
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
