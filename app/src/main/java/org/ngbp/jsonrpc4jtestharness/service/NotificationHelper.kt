package org.ngbp.jsonrpc4jtestharness.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.ngbp.jsonrpc4jtestharness.WithoutUIActivity
import org.ngbp.jsonrpc4jtestharness.MainActivity
import org.ngbp.jsonrpc4jtestharness.R
import org.ngbp.jsonrpc4jtestharness.controller.model.PlaybackState

class NotificationHelper {
    private var context: Context
    private var CHANNEL_ID: String = "ForegroundRpcServiceChannel"


    companion object {
        private val PLAYER_ACTION_PLAY = "player_action_play"
        private val PLAYER_ACTION_PAUSE = "player_action_pause"
        val PLAYER_ACTION = "player_action"
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
                .addAction(R.mipmap.ic_play, context.getString(R.string.play_btn_title), getPendingIntent(PLAYER_ACTION_PLAY, 0)) // #0
//                .addAction(R.mipmap.ic_play, context.getString(R.string.play_btn_title), getPendingIntent(PLAYER_ACTION_PLAY, PlaybackState.PLAYING.state)) // #0
                .addAction(R.mipmap.ic_pause, context.getString(R.string.pause_btn_title), getPendingIntent(PLAYER_ACTION_PAUSE, 1)) // #1
//                .addAction(R.mipmap.ic_pause, context.getString(R.string.pause_btn_title), getPendingIntent(PLAYER_ACTION_PAUSE, PlaybackState.PAUSED.state)) // #1
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
                .setContentIntent(pendingIntent)
                .build()
    }

    private fun getPendingIntent(intentAction: String, btnAction: Int): PendingIntent {
        return PendingIntent.getActivity(context, 0, getPlayerIntent(intentAction, btnAction), 0)
    }

    private fun getPlayerIntent(payerAction: String, btnAction: Int): Intent {
        return Intent(context, WithoutUIActivity::class.java).apply {
            action = payerAction
            putExtra(PLAYER_ACTION, btnAction)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
