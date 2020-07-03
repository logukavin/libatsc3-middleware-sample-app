package org.ngbp.jsonrpc4jtestharness.http.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import org.ngbp.jsonrpc4jtestharness.core.ws.GenerateSSLContext;
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebServer;
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebSocket;
import org.ngbp.jsonrpc4jtestharness.http.servers.ContentProviderServlet;

import java.util.Objects;

import androidx.annotation.Nullable;

import static org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebServer.Connectors.HTTPS_CONNECTOR;
import static org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWebServer.Connectors.WSS_CONNECTOR;

public class ForegroundRpcService extends Service {


    private PowerManager.WakeLock wakeLock;
    private Boolean isServiceStarted = false;
    public final static String ACTION_START = "START";
    public final static String ACTION_STOP = "STOP";
    private NotificationHelper notificationHelper;
    private MiddlewareWebServer webServer;

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

        startWebServer();
    }

    private void startWebServer() {
        Thread serverThread = new Thread() {
            @Override
            public void run() {
                try {
                    webServer = new MiddlewareWebServer.Builder()
                            .hostName("localHost")
                            .httpPort(8080)
                            .httpsPort(8443)
                            .wsPort(9998)
                            .wssPort(9999)
                            .addServlet(new ContentProviderServlet(getApplicationContext()))
                            .addWebSocket(new MiddlewareWebSocket())
                            .sslContext(new GenerateSSLContext(getApplicationContext()))
                            .enableConnectors(new MiddlewareWebServer.Connectors[]{HTTPS_CONNECTOR, WSS_CONNECTOR})
                            .build();
                    webServer.start();
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

            if (webServer.getServer().isRunning()) {
                try {
                    webServer.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
