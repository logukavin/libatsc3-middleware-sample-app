package org.ngbp.jsonrpc4jtestharness.http.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import org.ngbp.jsonrpc4jtestharness.http.servers.SimpleJettyWebServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import androidx.annotation.Nullable;

public class ForegroundRpcService extends Service {


    private PowerManager.WakeLock wakeLock;
    private Boolean isServiceStarted = false;
    public final static String ACTION_START = "START";
    public final static String ACTION_STOP = "STOP";
    private NotificationHelper notificationHelper;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationHelper = new NotificationHelper(this);
        String message = intent.getStringExtra("inputExtra");
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_START:
                    startService(message);
                    break;
                case ACTION_STOP:
                    stopService();
                    break;
                default:
            }
        }

        return START_NOT_STICKY;
    }

    private void startService(String message) {
        if (isServiceStarted) return;
        isServiceStarted = true;
        wakeLock = ((PowerManager) Objects.requireNonNull(getSystemService(Context.POWER_SERVICE))).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundRpcService::lock");
        wakeLock.acquire();
        startForeground(1, notificationHelper.createNotification(message));

        startWebServer(this);
    }

    private void startWebServer(Context context) {

        Thread serverThread = new Thread() {
            @Override
            public void run() {

                //        Read web content from assets folder
                StringBuilder sb = new StringBuilder();
                try {
                    String content;
                    InputStream is = getAssets().open("GitHub.htm");
                    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));

                    while ((content = br.readLine()) != null) {
                        sb.append(content);
                    }
                    br.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                String finalContent = sb.toString();

                SimpleJettyWebServer server = new SimpleJettyWebServer(finalContent, context);
                try {
                    server.startup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        serverThread.start();
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
