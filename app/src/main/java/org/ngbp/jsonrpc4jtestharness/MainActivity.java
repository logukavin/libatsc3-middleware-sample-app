package org.ngbp.jsonrpc4jtestharness;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nmuzhichin.jsonrpc.model.request.CompleteRequest;
import com.github.nmuzhichin.jsonrpc.model.request.Request;
import com.github.nmuzhichin.jsonrpc.module.JsonRpcModule;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWSServer;
import org.ngbp.jsonrpc4jtestharness.http.service.ForegroundRpcService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCProcessor;
import org.ngbp.jsonrpc4jtestharness.rpc.filterCodes.model.GetFilterCodes;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapper.registerModule(new JsonRpcModule());

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               stopService();
            }
        });
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService();
            }
        });
        findViewById(R.id.server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startServer();
            }
        });
        RPCProcessor callWrapper = new RPCProcessor();
        List<String> requestParams = new ArrayList<>();

        final Request request = new CompleteRequest("2.0", 1L, "org.atsc.getFilterCodes", new HashMap<>());
         String json="";
        try {
           json = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        final Request request2 = new CompleteRequest("2.0", 2L, "org.atsc.query.service", new HashMap<>());
         String json2="";
        try {
            json2 = mapper.writeValueAsString(request2);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        GetFilterCodes val =   callWrapper.processRequest(json);
        requestParams.add(json);
        requestParams.add(json2);
        List<Object> composedResponses =   callWrapper.processRequest(requestParams);
    }

    private void startServer() {
//        InetSocketAddress inetSocketAddress = new InetSocketAddress(1);
//        try {
//            MiddlewareWSServer ws = new MiddlewareWSServer(1);
//
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(8080);
                    MiddlewareWSServer middlewareWSServer =  new MiddlewareWSServer(inetSocketAddress);
                    middlewareWSServer.start();
                    System.out.println( "ChatServer started on port: " + middlewareWSServer.getPort() );
                    Log.d("Socket","onOpen");
                    try {
                        WebSocketClient cc = new WebSocketClient(new URI( "ws://localhost:8080/" )) {
                            @Override
                            public void onOpen(ServerHandshake handshakedata) {
                                Log.d("Socket","onOpen");
                            }

                            @Override
                            public void onMessage(String message) {
                                Log.d("Socket","onMessage");
                            }

                            @Override
                            public void onClose(int code, String reason, boolean remote) {
                                Log.d("Socket","onClose");
                            }

                            @Override
                            public void onError(Exception ex) {
                                Log.d("Socket","onError");
                            }
                        };
                        cc.connect();
                        cc.send("Ololo");
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }

    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundRpcService.class);
        serviceIntent.setAction(ForegroundRpcService.START);
        serviceIntent.putExtra("inputExtra", "Foreground RPC Service Example in Android");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundRpcService.class);
        serviceIntent.setAction(ForegroundRpcService.STOP);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    protected void onDestroy() {
        stopService();
        super.onDestroy();
    }
}
