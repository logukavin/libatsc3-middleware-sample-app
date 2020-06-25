package org.ngbp.jsonrpc4jtestharness.http.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;

import java.util.Objects;

public class ForegroundRpcService extends Service {


    private PowerManager.WakeLock wakeLock;
    private Boolean isServiceStarted = false;
    private final static String START = "START";
    private final static String STOP = "STOP";
    private NotificationManager notificationManager;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = new NotificationManager(this);
        String input = intent.getStringExtra("inputExtra");
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case START:
                    startService(input);
                    break;
                case STOP:
                    stopService();
                    break;
                default:
            }
        }

        return START_NOT_STICKY;
    }

    private void startService(String input) {
        if (isServiceStarted) return;
        isServiceStarted = true;
        wakeLock = ((PowerManager) Objects.requireNonNull(getSystemService(Context.POWER_SERVICE))).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundRpcService::lock");
        wakeLock.acquire();
        startForeground(1, notificationManager.createNotification(input));
    }

    private void stopService() {
        if (isServiceStarted) {
            if (wakeLock != null) {
                wakeLock.release();
            }
            stopForeground(true);
            stopSelf();
        }
        isServiceStarted = false;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
