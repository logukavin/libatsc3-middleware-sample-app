package org.ngbp.jsonrpc4jtestharness.http.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.ngbp.jsonrpc4jtestharness.MainActivity;
import org.ngbp.jsonrpc4jtestharness.R;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ForegroundRpcService extends Service {

    public static final String CHANNEL_ID = "ForegroundRpcServiceChannel";
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Rpc Service")
                .setContentText(input)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        //do heavy work on a background thread
        /*Thread thread = new Thread() {
            @Override
            public void run() {
                SimpleJettyWebServer jettyServer = new SimpleJettyWebServer();
                try {
                    jettyServer.startup();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d("Server"," isStarted? - " + jettyServer.getServer().isStarted());
                Log.d("Server"," isStarting? - " + jettyServer.getServer().isStarting());
                Log.d("Server"," isRunning? - " + jettyServer.getServer().isRunning());
                Log.d("Server"," Uri = " + jettyServer.getServer().getURI());
            }
        };

        thread.start();*/

        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Rpc Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
