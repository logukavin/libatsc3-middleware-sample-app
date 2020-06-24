package org.ngbp.jsonrpc4jtestharness.http.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.ngbp.jsonrpc4jtestharness.MainActivity;
import org.ngbp.jsonrpc4jtestharness.R;

import java.util.Objects;

public class ForegroundRpcService extends Service {

    public static final String CHANNEL_ID = "ForegroundRpcServiceChannel";
    private PowerManager.WakeLock wakeLock;
    private Boolean isServiceStarted = false;
    private final static String START = "START";
    private final static String STOP = "STOP";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case START:
                    startService(input);
                    break;
                case STOP:
                    stopService();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + intent.getAction());
            }
        }

        return START_NOT_STICKY;
    }

    private void startService(String input) {
        if (isServiceStarted) return;
        isServiceStarted = true;
        wakeLock = ((PowerManager) Objects.requireNonNull(getSystemService(Context.POWER_SERVICE))).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundRpcService::lock");
        wakeLock.acquire();
        startForeground(1, createNotification(input));
    }

    private void stopService() {
        try {
            if (wakeLock != null) {
                wakeLock.release();
            }
            stopForeground(true);
            stopSelf();
        } catch (Exception e) {
            Log.d("ForegroundRpcService", "Service stopped without being started: ${e.message}");
        }
        isServiceStarted = false;

    }

    private Notification createNotification(String input) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Rpc Service")
                .setContentText(input)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();
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
