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
import org.ngbp.jsonrpc4jtestharness.core.ws.IOnRequest;
import org.ngbp.jsonrpc4jtestharness.core.ws.MiddlewareWSServer;
import org.ngbp.jsonrpc4jtestharness.http.service.ForegroundRpcService;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.IOnMessageListener;
import org.ngbp.jsonrpc4jtestharness.jsonrpc2.RPCProcessor;

import java.net.InetSocketAddress;
import java.net.URI;
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
            }
        });
        RPCProcessor callWrapper = new RPCProcessor();
        List<String> requestParams = new ArrayList<>();

        final Request request = new CompleteRequest("2.0", 1L, "org.atsc.getFilterCodes", new HashMap<>());
        String json = "";
        try {
            json = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        final Request request2 = new CompleteRequest("2.0", 2L, "org.atsc.query.service", new HashMap<>());
        String json2 = "";
        try {
            json2 = mapper.writeValueAsString(request2);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String val = callWrapper.processRequest(json);
        requestParams.add(json);
        requestParams.add(json2);
        List<String> composedResponses = callWrapper.processRequest(requestParams);
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
