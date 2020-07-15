package org.ngbp.jsonrpc4jtestharness.service

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
        val prevPendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, 0)
        val pausePendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, 0)

        return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Foreground Rpc Service")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .addAction(R.mipmap.ic_play, "Play", prevPendingIntent) // #0
                .addAction(R.mipmap.ic_pause, "Pause", pausePendingIntent) // #1
//                .setStyle(MediaNotificationCompat.MediaStyle()
//                        .setShowActionsInCompactView(1 /* #1: pause button \*/)
//                        .setMediaSession(mediaSession.getSessionToken()))
                .setContentIntent(pendingIntent)
                .build()
    }


    //    var notification = NotificationCompat.Builder(context, CHANNEL_ID)
//            // Show controls on lock screen even when user hides sensitive content.
//            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//            .setSmallIcon(R.drawable.ic_stat_player)
//            // Add media control buttons that invoke intents in your media service
//            .addAction(R.drawable.ic_prev, "Previous", prevPendingIntent) // #0
//            .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent) // #1
//            .addAction(R.drawable.ic_next, "Next", nextPendingIntent) // #2
//            // Apply the media style template
//            .setStyle(MediaNotificationCompat.MediaStyle()
//                    .setShowActionsInCompactView(1 /* #1: pause button \*/)
//                    .setMediaSession(mediaSession.getSessionToken()))
//            .setContentTitle("Wonderful music")
//            .setContentText("My Awesome Band")
//            .setLargeIcon(albumArtBitmap)
//            .build()
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