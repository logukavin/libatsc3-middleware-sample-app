package org.ngbp.jsonrpc4jtestharness.http.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.ngbp.jsonrpc4jtestharness.core.ws.EchoSocket;
import org.ngbp.jsonrpc4jtestharness.http.servers.SimpleJettyWebServer;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.Future;

import androidx.annotation.Nullable;

public class ForegroundRpcService extends Service {


    private PowerManager.WakeLock wakeLock;
    private Boolean isServiceStarted = false;
    public final static String ACTION_START = "START";
    public final static String ACTION_STOP = "STOP";
    private NotificationHelper notificationHelper;
    private SimpleJettyWebServer webServer;

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
        startWSClient();
    }

    private void startWebServer(Context context) {

        Thread serverThread = new Thread() {
            @Override
            public void run() {

                webServer = new SimpleJettyWebServer(context);
                try {
                    webServer.runWebServer();
                    Log.d("Test", " URL: " + webServer.getServer().getURI());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        serverThread.start();
    }

    private void startWSClient() {
        Thread client = new Thread() {
            @Override
            public void run() {
                startClient();
            }
        };

        client.start();
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

    public void startClient() {
        URI uri = URI.create("ws://localhost:8080/github");

        WebSocketClient client = new WebSocketClient();
        try {
            try {
                client.start();
                // The socket that receives events
                EchoSocket socket = new EchoSocket();
                // Attempt Connect
                Future<Session> fut = client.connect(socket,uri);
                // Wait for Connect
                Session session = fut.get();
                // Send a message
                session.getRemote().sendString("Hello");
                // Close session
                session.close();
            } finally {
                client.stop();
            }
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
}
