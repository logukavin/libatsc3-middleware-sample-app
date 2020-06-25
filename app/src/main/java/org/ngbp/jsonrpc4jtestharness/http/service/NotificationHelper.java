package org.ngbp.jsonrpc4jtestharness.http.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import org.ngbp.jsonrpc4jtestharness.MainActivity;
import org.ngbp.jsonrpc4jtestharness.R;


public class NotificationHelper {

    private Context context;
    private String CHANNEL_ID = "ForegroundRpcServiceChannel";

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    public NotificationHelper(Context context, String channelID) {
        this.context = context;
        this.CHANNEL_ID = channelID;
        createNotificationChannel();
    }

    public Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, 0);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Foreground Rpc Service")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Rpc Service Channel",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
        );
        android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

}
